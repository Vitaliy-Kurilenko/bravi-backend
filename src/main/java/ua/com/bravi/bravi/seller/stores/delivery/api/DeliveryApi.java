package ua.com.bravi.bravi.seller.stores.delivery.api;

import java.util.List;
import java.util.Map;

public interface DeliveryApi {

    List<DeliveryMethodDefinitionView> listAvailableMethods();

    List<StoreDeliveryMethodView> findByStoreId(Long storeId);

    List<StoreDeliveryMethodView> findEnabledByStoreId(Long storeId);

    void enableMethod(Long storeId, String methodCode, Map<String, String> config);

    void updateMethodConfig(Long storeId, String methodCode, Map<String, String> config);

    void disableMethod(Long storeId, String methodCode);
}
