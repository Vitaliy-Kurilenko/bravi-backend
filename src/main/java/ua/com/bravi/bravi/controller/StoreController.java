package ua.com.bravi.bravi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.com.bravi.bravi.controller.dto.in.StoreCreateRequest;
import ua.com.bravi.bravi.controller.dto.in.StoreUpdateRequest;
import ua.com.bravi.bravi.controller.dto.out.StoreResponse;
import ua.com.bravi.bravi.controller.mapper.StoreDtoMapper;
import ua.com.bravi.bravi.service.StoreService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/stores")
@Tag(name = "StoreController")
public class StoreController {

    private final StoreService storeService;
    private final StoreDtoMapper storeDtoMapper;

    @Operation(summary = "Get store", description = "Returns the current user's store")
    @GetMapping
    public StoreResponse getStore() {
        return storeDtoMapper.toResponse(storeService.getCurrentUserStore());
    }

    @Operation(summary = "Create store", description = "Creates a store for the current user")
    @PostMapping
    public ResponseEntity<Void> createStore(@Valid @RequestBody StoreCreateRequest request) {
        storeService.createStore(storeDtoMapper.toDomain(request));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Update store", description = "Partially updates the current user's store")
    @PatchMapping
    public ResponseEntity<Void> updateStore(@Valid @RequestBody StoreUpdateRequest request) {
        storeService.updateCurrentUserStore(storeDtoMapper.toDomain(request));
        return ResponseEntity.noContent().build();
    }
}
