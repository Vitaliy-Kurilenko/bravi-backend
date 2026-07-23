package ua.com.bravi.bravi.shared.common;

/** Порядок MVC-інтерсепторів між модулями. Хто читає InvocationContext — після того, хто його заповнює. */
public final class InterceptorOrder {

    public static final int CURRENT_USER = 0;              // заповнює InvocationContext.userId
    public static final int RESOLVE_ACCOUNT_CONTEXT = 50;  // X-Account-Id → AccountContext
    public static final int RESOLVE_STORE_CONTEXT = 60;    // X-Store-Id → StoreContext (після account)
    public static final int STORE_REQUIRED = 100;          // читає store — після RESOLVE_STORE_CONTEXT

    private InterceptorOrder() {
    }
}
