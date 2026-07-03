package ua.com.bravi.bravi.seller.stores.delivery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import ua.com.bravi.bravi.shared.exception.NotFoundException;
import ua.com.bravi.bravi.seller.stores.delivery.exception.UnknownDeliveryMethodException;
import ua.com.bravi.bravi.seller.stores.delivery.persistence.IStoreDeliveryMethodRepository;
import ua.com.bravi.bravi.seller.stores.delivery.persistence.entity.StoreDeliveryMethodEntity;
import ua.com.bravi.bravi.seller.stores.delivery.persistence.mapper.StoreDeliveryMethodEntityMapper;
import ua.com.bravi.bravi.seller.stores.delivery.registry.DeliveryMethodProvider;
import ua.com.bravi.bravi.seller.stores.delivery.registry.DeliveryMethodRegistry;
import ua.com.bravi.bravi.seller.stores.delivery.registry.providers.SelfPickupProvider;

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
class DeliveryServiceTest {

    private static final Long STORE_ID = 7L;
    private static final String CODE = "SELF_PICKUP";

    private final IStoreDeliveryMethodRepository repository = mock(IStoreDeliveryMethodRepository.class);
    private final StoreDeliveryMethodEntityMapper mapper = mock(StoreDeliveryMethodEntityMapper.class);

    private DeliveryService service;

    @BeforeEach
    void setUp() {
        DeliveryMethodProvider pickup = new SelfPickupProvider();
        DeliveryMethodRegistry registry = new DeliveryMethodRegistry(List.of(pickup));
        service = new DeliveryService(repository, mapper, registry);
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

        service.enableMethod(STORE_ID, CODE, Map.of("note", "back door"));

        ArgumentCaptor<StoreDeliveryMethodEntity> captor = ArgumentCaptor.forClass(StoreDeliveryMethodEntity.class);
        verify(repository).save(captor.capture());
        StoreDeliveryMethodEntity saved = captor.getValue();
        assertThat(saved.getStoreId()).isEqualTo(STORE_ID);
        assertThat(saved.getMethodCode()).isEqualTo(CODE);
        assertThat(saved.getEnabled()).isTrue();
        assertThat(saved.getConfig()).containsEntry("note", "back door");
    }

    @Test
    void enableMethodUpdatesExistingEntity() {
        StoreDeliveryMethodEntity existing = new StoreDeliveryMethodEntity();
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
        assertThatThrownBy(() -> service.enableMethod(STORE_ID, "NOVA_POSHTA", Map.of()))
                .isInstanceOf(UnknownDeliveryMethodException.class);

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
        StoreDeliveryMethodEntity existing = new StoreDeliveryMethodEntity();
        existing.setStoreId(STORE_ID);
        existing.setMethodCode(CODE);
        existing.setEnabled(true);
        existing.setConfig(new java.util.HashMap<>(Map.of("note", "keep me")));
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
