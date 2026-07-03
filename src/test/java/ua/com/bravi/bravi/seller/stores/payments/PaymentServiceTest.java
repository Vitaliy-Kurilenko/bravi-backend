package ua.com.bravi.bravi.seller.stores.payments;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import ua.com.bravi.bravi.shared.exception.NotFoundException;
import ua.com.bravi.bravi.seller.stores.payments.exception.UnknownPaymentMethodException;
import ua.com.bravi.bravi.seller.stores.payments.persistence.IStorePaymentMethodRepository;
import ua.com.bravi.bravi.seller.stores.payments.persistence.entity.StorePaymentMethodEntity;
import ua.com.bravi.bravi.seller.stores.payments.persistence.mapper.StorePaymentMethodEntityMapper;
import ua.com.bravi.bravi.seller.stores.payments.registry.PaymentMethodProvider;
import ua.com.bravi.bravi.seller.stores.payments.registry.PaymentMethodRegistry;
import ua.com.bravi.bravi.seller.stores.payments.registry.providers.CashOnDeliveryProvider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final Long STORE_ID = 7L;
    private static final String CODE = "CASH_ON_DELIVERY";

    private final IStorePaymentMethodRepository repository = mock(IStorePaymentMethodRepository.class);
    private final StorePaymentMethodEntityMapper mapper = mock(StorePaymentMethodEntityMapper.class);

    private PaymentService service;

    @BeforeEach
    void setUp() {
        PaymentMethodProvider cash = new CashOnDeliveryProvider();
        PaymentMethodRegistry registry = new PaymentMethodRegistry(List.of(cash));
        service = new PaymentService(repository, mapper, registry);
    }

    @Test
    void listAvailableMethodsReflectsRegistry() {
        assertThat(service.listAvailableMethods())
                .singleElement()
                .satisfies(definition -> {
                    assertThat(definition.code()).isEqualTo(CODE);
                    assertThat(definition.configSchema()).isEmpty();
                });
    }

    @Test
    void enableMethodCreatesEntityWhenAbsent() {
        when(repository.findByStoreIdAndMethodCode(STORE_ID, CODE)).thenReturn(Optional.empty());

        service.enableMethod(STORE_ID, CODE, Map.of("note", "exact change"));

        ArgumentCaptor<StorePaymentMethodEntity> captor = ArgumentCaptor.forClass(StorePaymentMethodEntity.class);
        verify(repository).save(captor.capture());
        StorePaymentMethodEntity saved = captor.getValue();
        assertThat(saved.getStoreId()).isEqualTo(STORE_ID);
        assertThat(saved.getMethodCode()).isEqualTo(CODE);
        assertThat(saved.getEnabled()).isTrue();
        assertThat(saved.getConfig()).containsEntry("note", "exact change");
    }

    @Test
    void enableMethodUpdatesExistingEntity() {
        StorePaymentMethodEntity existing = new StorePaymentMethodEntity();
        existing.setStoreId(STORE_ID);
        existing.setMethodCode(CODE);
        existing.setEnabled(false);
        when(repository.findByStoreIdAndMethodCode(STORE_ID, CODE)).thenReturn(Optional.of(existing));

        service.enableMethod(STORE_ID, CODE, null);

        verify(repository).save(existing);
        assertThat(existing.getEnabled()).isTrue();
        assertThat(existing.getConfig()).isEmpty();
    }

    @Test
    void enableMethodRejectsUnknownCode() {
        assertThatThrownBy(() -> service.enableMethod(STORE_ID, "STRIPE", Map.of()))
                .isInstanceOf(UnknownPaymentMethodException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void updateMethodConfigThrowsWhenNotEnabled() {
        when(repository.findByStoreIdAndMethodCode(STORE_ID, CODE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateMethodConfig(STORE_ID, CODE, Map.of()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void disableMethodClearsEnabledFlagButKeepsConfig() {
        StorePaymentMethodEntity existing = new StorePaymentMethodEntity();
        existing.setStoreId(STORE_ID);
        existing.setMethodCode(CODE);
        existing.setEnabled(true);
        existing.setConfig(new HashMap<>(Map.of("note", "keep me")));
        when(repository.findByStoreIdAndMethodCode(STORE_ID, CODE)).thenReturn(Optional.of(existing));

        service.disableMethod(STORE_ID, CODE);

        assertThat(existing.getEnabled()).isFalse();
        assertThat(existing.getConfig()).containsEntry("note", "keep me");
    }

    @Test
    void disableMethodThrowsWhenNotEnabled() {
        when(repository.findByStoreIdAndMethodCode(STORE_ID, CODE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.disableMethod(STORE_ID, CODE))
                .isInstanceOf(NotFoundException.class);
    }
}
