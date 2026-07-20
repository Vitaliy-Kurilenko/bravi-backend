package ua.com.bravi.bravi.seller.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ua.com.bravi.bravi.seller.controller.dto.in.StoreUpdateRequest;
import ua.com.bravi.bravi.seller.controller.dto.out.StoreResponse;
import ua.com.bravi.bravi.seller.controller.mapper.StoreDtoMapper;
import ua.com.bravi.bravi.seller.controller.mapper.StoreLogoDtoMapper;
import ua.com.bravi.bravi.seller.stores.api.StoreView;
import ua.com.bravi.bravi.seller.stores.api.StoresApi;
import ua.com.bravi.bravi.seller.stores.api.CurrentStoreHolder;
import ua.com.bravi.bravi.seller.stores.domain.Store;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StoreControllerTest {

    private final StoresApi storesApi = mock(StoresApi.class);
    private final StoreDtoMapper storeDtoMapper = mock(StoreDtoMapper.class);
    private final StoreLogoDtoMapper storeLogoDtoMapper = mock(StoreLogoDtoMapper.class);
    private final CurrentStoreHolder currentStoreHolder = mock(CurrentStoreHolder.class);
    private final SellerStoreController controller =
            new SellerStoreController(storesApi, storeDtoMapper, storeLogoDtoMapper, currentStoreHolder);

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
        verify(storesApi, never()).confirmLogo(anyLong(), any());
    }

    @Test
    void updateStoreAttachesLogoWhenStorageKeyPresent() {
        StoreUpdateRequest request = new StoreUpdateRequest(
                "NewName", null, null, null, null,
                null, null, null,
                null, null, null, null, "store-logos/42/a.png"
        );
        Store domain = mock(Store.class);
        when(storeDtoMapper.toDomain(request)).thenReturn(domain);
        when(currentStoreHolder.get()).thenReturn(42L);

        controller.updateStore(request);

        verify(storesApi).updateStore(42L, domain);
        verify(storesApi).confirmLogo(42L, "store-logos/42/a.png");
    }
}
