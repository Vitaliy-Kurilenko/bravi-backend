package ua.com.bravi.bravi.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ua.com.bravi.bravi.controller.dto.in.StoreCreateRequest;
import ua.com.bravi.bravi.controller.dto.in.StoreUpdateRequest;
import ua.com.bravi.bravi.controller.dto.out.StoreResponse;
import ua.com.bravi.bravi.controller.mapper.StoreDtoMapper;
import ua.com.bravi.bravi.domain.store.Store;
import ua.com.bravi.bravi.service.StoreService;

import java.time.ZoneId;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StoreControllerTest {

    private final StoreService storeService = mock(StoreService.class);
    private final StoreDtoMapper storeDtoMapper = mock(StoreDtoMapper.class);
    private final StoreController controller = new StoreController(storeService, storeDtoMapper);

    @Test
    void getStoreReturnsMappedResponse() {
        Store store = mock(Store.class);
        StoreResponse response = mock(StoreResponse.class);
        when(storeService.getCurrentUserStore()).thenReturn(store);
        when(storeDtoMapper.toResponse(store)).thenReturn(response);

        StoreResponse result = controller.getStore();

        assertThat(result).isSameAs(response);
        verify(storeService).getCurrentUserStore();
    }

    @Test
    void createStoreReturns201AndDelegatesToService() {
        StoreCreateRequest request = new StoreCreateRequest(
                "Shop", null, null, null, null,
                null, null, null,
                ZoneId.of("UTC"), null, null,
                Currency.getInstance("UAH"), true
        );
        Store domain = mock(Store.class);
        when(storeDtoMapper.toDomain(request)).thenReturn(domain);

        ResponseEntity<Void> result = controller.createStore(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(storeService).createStore(domain);
    }

    @Test
    void updateStoreReturns204AndDelegatesToService() {
        StoreUpdateRequest request = new StoreUpdateRequest(
                "NewName", null, null, null, null,
                null, null, null,
                null, null, null, null, null
        );
        Store domain = mock(Store.class);
        when(storeDtoMapper.toDomain(request)).thenReturn(domain);

        ResponseEntity<Void> result = controller.updateStore(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(storeService).updateCurrentUserStore(domain);
    }
}
