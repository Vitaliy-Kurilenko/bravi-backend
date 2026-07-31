package ua.com.bravi.bravi.shared.common;

/** Ordering of MVC interceptors across modules: an interceptor that reads a context runs after the one that fills it. */
public final class InterceptorOrder {

    public static final int CURRENT_USER = 0;              // fills InvocationContext.userId
    public static final int RESOLVE_ACCOUNT_CONTEXT = 50;  // X-Account-Id → AccountContext
    public static final int RESOLVE_STORE_CONTEXT = 60;    // X-Store-Id → StoreContext (after the account)
    public static final int STORE_REQUIRED = 100;          // reads the store resolved by RESOLVE_STORE_CONTEXT

    private InterceptorOrder() {
    }
}
