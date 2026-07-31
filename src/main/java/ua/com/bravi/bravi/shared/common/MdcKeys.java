package ua.com.bravi.bravi.shared.common;

/**
 * MDC keys that filters put into the diagnostic context of a request.
 * In a structured profile each of them becomes a separate JSON field.
 * Only indirect identifiers belong here — no personal data such as an email, a name or a username.
 */
public final class MdcKeys {

    public static final String REQUEST_ID = "requestId";
    public static final String ACCOUNT_ID = "accountId";
    public static final String STORE_ID = "storeId";
    public static final String USER_EXT_ID = "userExtId";

    private MdcKeys() {
    }
}
