package ua.com.bravi.bravi.seller.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ua.com.bravi.bravi.seller.controller.dto.in.StoreCreateRequest;
import ua.com.bravi.bravi.seller.controller.dto.in.StoreUpdateRequest;
import ua.com.bravi.bravi.seller.controller.dto.out.StoreResponse;
import ua.com.bravi.bravi.seller.controller.mapper.StoreDtoMapper;
import ua.com.bravi.bravi.access.api.CurrentAccountHolder;
import ua.com.bravi.bravi.seller.stores.api.StoreView;
import ua.com.bravi.bravi.seller.stores.api.StoresApi;
import ua.com.bravi.bravi.seller.stores.api.CurrentStoreHolder;
import ua.com.bravi.bravi.seller.stores.domain.Store;

import java.time.ZoneId;
import java.util.Currency;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StoreControllerTest {

    private final StoresApi storesApi = mock(StoresApi.class);
    private final StoreDtoMapper storeDtoMapper = mock(StoreDtoMapper.class);
    private final CurrentAccountHolder currentAccountHolder = mock(CurrentAccountHolder.class);
    private final CurrentStoreHolder currentStoreHolder = mock(CurrentStoreHolder.class);
    private final SellerStoreController controller =
            new SellerStoreController(storesApi, storeDtoMapper, currentAccountHolder, currentStoreHolder);

    @Test
    void getStoreReturnsMappedResponse() {
        StoreView view = mock(StoreView.class);
        StoreResponse response = mock(StoreResponse.class);
        when(currentStoreHolder.get()).thenReturn(42L);
        when(storesApi.getStoreById(42L)).thenReturn(Optional.of(view));
        when(storeDtoMapper.toResponse(view)).thenReturn(response);

        StoreResponse result = controller.getStore();

        assertThat(result).isSameAs(response);
    }

    @Test
    void createStoreReturns201AndDelegatesToApi() {
        StoreCreateRequest request = new StoreCreateRequest(
                "Shop", null, null, null, null,
                null, null, null,
                ZoneId.of("UTC"), null, null,
                Currency.getInstance("UAH"), true
        );
        Store domain = mock(Store.class);
        when(storeDtoMapper.toDomain(request)).thenReturn(domain);
        when(currentAccountHolder.getAccountId()).thenReturn(7L);
        when(storesApi.createStore(7L, domain)).thenReturn(100L);

        ResponseEntity<Void> result = controller.createStore(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(storesApi).createStore(7L, domain);
        verify(currentStoreHolder).set(100L);
    }

    @Test
    void updateStoreReturns204AndDelegatesToApi() {
        StoreUpdateRequest request = new StoreUpdateRequest(
                "NewName", null, null, null, null,
                null, null, null,
                null, null, null, null, null
        );
        Store domain = mock(Store.class);
        when(storeDtoMapper.toDomain(request)).thenReturn(domain);
        when(currentStoreHolder.get()).thenReturn(42L);

        ResponseEntity<Void> result = controller.updateStore(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(storesApi).updateStore(42L, domain);
    }
}
