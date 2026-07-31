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
 * Logs arguments and results of public calls on {@code @Service} beans, including cross-module ones,
 * since modules talk to each other through a {@code <Module>Api} implemented by a root service.
 * Stays off until the {@link LoggingConstants#SERVICE_CALL_LOGGER} logger is set to DEBUG:
 * {@code isDebugEnabled()} short-circuits the advice before any argument is processed.
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
            // The exception itself is logged by the exception handler; only its type is traced here.
            log.debug("<-- {} threw {} in {}ms",
                    target, failure.getClass().getSimpleName(), elapsedMs(start));
            throw failure;
        }
    }

    /**
     * Renders arguments as {@code name=value}: masking is driven by the parameter name,
     * which a bare scalar value does not carry.
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
