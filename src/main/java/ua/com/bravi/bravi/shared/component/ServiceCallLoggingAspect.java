package ua.com.bravi.bravi.shared.component;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ua.com.bravi.bravi.shared.common.LoggingConstants;
import ua.com.bravi.bravi.shared.util.LogSanitizer;

import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Логує параметри та результати публічних викликів {@code @Service}-бінів — зокрема
 * міжмодульних, бо модулі спілкуються через {@code <Module>Api}, який реалізує кореневий сервіс.
 * Вимкнений, доки логер {@link LoggingConstants#SERVICE_CALL_LOGGER} не переведено в DEBUG:
 * тоді {@code isDebugEnabled()} відсікає виклик до будь-якої обробки аргументів.
 */
@Aspect
public class ServiceCallLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingConstants.SERVICE_CALL_LOGGER);

    @Around("@within(org.springframework.stereotype.Service) && execution(public * *(..))")
    public Object logCall(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!log.isDebugEnabled()) {
            return joinPoint.proceed();
        }

        String target = joinPoint.getSignature().getDeclaringType().getSimpleName()
                + "." + joinPoint.getSignature().getName();
        log.debug("--> {}({})", target, arguments(joinPoint));

        long start = System.nanoTime();
        try {
            Object result = joinPoint.proceed();
            log.debug("<-- {} returned {} in {}ms",
                    target, LogSanitizer.describe(result), elapsedMs(start));
            return result;
        } catch (Throwable failure) {
            // Сам виняток логується в exception-handler'і; тут лише факт і тип для трасування виклику.
            log.debug("<-- {} threw {} in {}ms",
                    target, failure.getClass().getSimpleName(), elapsedMs(start));
            throw failure;
        }
    }

    /**
     * Рендерить аргументи як {@code name=value}: без імені параметра скалярний рядок
     * (напр. email, переданий позиційно) не має за чим маскуватись.
     */
    private String arguments(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        String[] names = parameterNames(joinPoint, args.length);
        return IntStream.range(0, args.length)
                .mapToObj(i -> LogSanitizer.sanitize(names[i] + "=" + LogSanitizer.describe(args[i])))
                .collect(Collectors.joining(", "));
    }

    private String[] parameterNames(ProceedingJoinPoint joinPoint, int count) {
        if (joinPoint.getSignature() instanceof MethodSignature method) {
            String[] names = method.getParameterNames();
            if (names != null && names.length == count) {
                return names;
            }
        }
        return IntStream.range(0, count).mapToObj(i -> "arg" + i).toArray(String[]::new);
    }

    private long elapsedMs(long startNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }
}
