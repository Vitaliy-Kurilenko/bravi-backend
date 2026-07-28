package ua.com.bravi.bravi.shared.common;

/**
 * MDC-ключі, які фільтри кладуть у діагностичний контекст запиту.
 * У structured-профілі кожен із них стає окремим полем JSON.
 * Кладемо лише непрямі ідентифікатори — жодних PII (email, ім'я, username).
 */
public final class MdcKeys {

    public static final String REQUEST_ID = "requestId";
    public static final String ACCOUNT_ID = "accountId";
    public static final String STORE_ID = "storeId";
    public static final String USER_EXT_ID = "userExtId";

    private MdcKeys() {
    }
}
