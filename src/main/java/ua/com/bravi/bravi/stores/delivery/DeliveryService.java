package ua.com.bravi.bravi.stores.delivery;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.com.bravi.bravi.shared.exception.NotFoundException;
import ua.com.bravi.bravi.stores.delivery.api.DeliveryApi;
import ua.com.bravi.bravi.stores.delivery.api.DeliveryMethodDefinitionView;
import ua.com.bravi.bravi.stores.delivery.api.StoreDeliveryMethodView;
import ua.com.bravi.bravi.stores.delivery.persistence.IStoreDeliveryMethodRepository;
import ua.com.bravi.bravi.stores.delivery.persistence.entity.StoreDeliveryMethodEntity;
import ua.com.bravi.bravi.stores.delivery.persistence.mapper.StoreDeliveryMethodEntityMapper;
import ua.com.bravi.bravi.stores.delivery.registry.DeliveryMethodProvider;
import ua.com.bravi.bravi.stores.delivery.registry.DeliveryMethodRegistry;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DeliveryService implements DeliveryApi {

    private final IStoreDeliveryMethodRepository deliveryMethodRepository;
    private final StoreDeliveryMethodEntityMapper deliveryMethodEntityMapper;
    private final DeliveryMethodRegistry deliveryMethodRegistry;

    @Override
    public List<DeliveryMethodDefinitionView> listAvailableMethods() {
        return deliveryMethodRegistry.all().stream()
                .map(this::toDefinition)
                .toList();
    }

    @Override
    public List<StoreDeliveryMethodView> findByStoreId(Long storeId) {
        return deliveryMethodEntityMapper.toViews(deliveryMethodRepository.findByStoreId(storeId));
    }

    @Override
    public List<StoreDeliveryMethodView> findEnabledByStoreId(Long storeId) {
        return deliveryMethodEntityMapper.toViews(deliveryMethodRepository.findByStoreIdAndEnabledTrue(storeId));
    }

    @Override
    @Transactional
    public void enableMethod(Long storeId, String methodCode, Map<String, String> config) {
        deliveryMethodRegistry.get(methodCode).validateConfig(config);

        StoreDeliveryMethodEntity entity = deliveryMethodRepository
                .findByStoreIdAndMethodCode(storeId, methodCode)
                .orElseGet(() -> newEntity(storeId, methodCode));

        entity.setConfig(config != null ? config : Map.of());
        entity.setEnabled(true);

        deliveryMethodRepository.save(entity);
    }

    @Override
    @Transactional
    public void updateMethodConfig(Long storeId, String methodCode, Map<String, String> config) {
        deliveryMethodRegistry.get(methodCode).validateConfig(config);

        StoreDeliveryMethodEntity entity = requireEnabledMethod(storeId, methodCode);
        entity.setConfig(config != null ? config : Map.of());
    }

    @Override
    @Transactional
    public void disableMethod(Long storeId, String methodCode) {
        StoreDeliveryMethodEntity entity = requireEnabledMethod(storeId, methodCode);
        entity.setEnabled(false);
    }

    private StoreDeliveryMethodEntity requireEnabledMethod(Long storeId, String methodCode) {
        return deliveryMethodRepository.findByStoreIdAndMethodCode(storeId, methodCode)
                .orElseThrow(() -> new NotFoundException("Delivery method is not enabled for this store"));
    }

    private StoreDeliveryMethodEntity newEntity(Long storeId, String methodCode) {
        StoreDeliveryMethodEntity entity = new StoreDeliveryMethodEntity();
        entity.setStoreId(storeId);
        entity.setMethodCode(methodCode);
        return entity;
    }

    private DeliveryMethodDefinitionView toDefinition(DeliveryMethodProvider provider) {
        return new DeliveryMethodDefinitionView(provider.code(), provider.displayName(), provider.configSchema());
    }
}
