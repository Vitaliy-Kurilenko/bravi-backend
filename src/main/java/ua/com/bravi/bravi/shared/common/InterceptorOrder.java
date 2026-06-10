package ua.com.bravi.bravi.shared.common;

/** Порядок MVC-інтерсепторів між модулями. Хто читає InvocationContext — після того, хто його заповнює. */
public final class InterceptorOrder {

    public static final int CURRENT_USER = 0;       // заповнює InvocationContext.userId
    public static final int STORE_REQUIRED = 100;   // читає userId — після CURRENT_USER

    private InterceptorOrder() {
    }
}
