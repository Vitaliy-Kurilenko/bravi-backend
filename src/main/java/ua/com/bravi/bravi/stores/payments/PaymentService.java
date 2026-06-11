package ua.com.bravi.bravi.stores.payments;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.com.bravi.bravi.shared.exception.NotFoundException;
import ua.com.bravi.bravi.stores.payments.api.PaymentMethodDefinitionView;
import ua.com.bravi.bravi.stores.payments.api.PaymentsApi;
import ua.com.bravi.bravi.stores.payments.api.StorePaymentMethodView;
import ua.com.bravi.bravi.stores.payments.persistence.IStorePaymentMethodRepository;
import ua.com.bravi.bravi.stores.payments.persistence.entity.StorePaymentMethodEntity;
import ua.com.bravi.bravi.stores.payments.persistence.mapper.StorePaymentMethodEntityMapper;
import ua.com.bravi.bravi.stores.payments.registry.PaymentMethodProvider;
import ua.com.bravi.bravi.stores.payments.registry.PaymentMethodRegistry;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentService implements PaymentsApi {

    private final IStorePaymentMethodRepository paymentMethodRepository;
    private final StorePaymentMethodEntityMapper paymentMethodEntityMapper;
    private final PaymentMethodRegistry paymentMethodRegistry;

    @Override
    public List<PaymentMethodDefinitionView> listAvailableMethods() {
        return paymentMethodRegistry.all().stream()
                .map(this::toDefinition)
                .toList();
    }

    @Override
    public List<StorePaymentMethodView> findByStoreId(Long storeId) {
        return paymentMethodEntityMapper.toViews(paymentMethodRepository.findByStoreId(storeId));
    }

    @Override
    public List<StorePaymentMethodView> findEnabledByStoreId(Long storeId) {
        return paymentMethodEntityMapper.toViews(paymentMethodRepository.findByStoreIdAndEnabledTrue(storeId));
    }

    @Override
    @Transactional
    public void enableMethod(Long storeId, String methodCode, Map<String, String> config) {
        paymentMethodRegistry.get(methodCode).validateConfig(config);

        StorePaymentMethodEntity entity = paymentMethodRepository
                .findByStoreIdAndMethodCode(storeId, methodCode)
                .orElseGet(() -> newEntity(storeId, methodCode));

        entity.setConfig(config != null ? config : Map.of());
        entity.setEnabled(true);

        paymentMethodRepository.save(entity);
    }

    @Override
    @Transactional
    public void updateMethodConfig(Long storeId, String methodCode, Map<String, String> config) {
        paymentMethodRegistry.get(methodCode).validateConfig(config);

        StorePaymentMethodEntity entity = requireEnabledMethod(storeId, methodCode);
        entity.setConfig(config != null ? config : Map.of());
    }

    @Override
    @Transactional
    public void disableMethod(Long storeId, String methodCode) {
        StorePaymentMethodEntity entity = requireEnabledMethod(storeId, methodCode);
        entity.setEnabled(false);
    }

    private StorePaymentMethodEntity requireEnabledMethod(Long storeId, String methodCode) {
        return paymentMethodRepository.findByStoreIdAndMethodCode(storeId, methodCode)
                .orElseThrow(() -> new NotFoundException("Payment method is not enabled for this store"));
    }

    private StorePaymentMethodEntity newEntity(Long storeId, String methodCode) {
        StorePaymentMethodEntity entity = new StorePaymentMethodEntity();
        entity.setStoreId(storeId);
        entity.setMethodCode(methodCode);
        return entity;
    }

    private PaymentMethodDefinitionView toDefinition(PaymentMethodProvider provider) {
        return new PaymentMethodDefinitionView(provider.code(), provider.displayName(), provider.configSchema());
    }
}
