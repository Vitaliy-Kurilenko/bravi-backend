package ua.com.bravi.bravi.stores.payments.api;

import java.util.List;
import java.util.Map;

public interface PaymentsApi {

    List<PaymentMethodDefinitionView> listAvailableMethods();

    List<StorePaymentMethodView> findByStoreId(Long storeId);

    List<StorePaymentMethodView> findEnabledByStoreId(Long storeId);

    void enableMethod(Long storeId, String methodCode, Map<String, String> config);

    void updateMethodConfig(Long storeId, String methodCode, Map<String, String> config);

    void disableMethod(Long storeId, String methodCode);
}
