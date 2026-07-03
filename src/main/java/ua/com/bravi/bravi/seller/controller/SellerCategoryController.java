package ua.com.bravi.bravi.seller.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.com.bravi.bravi.seller.catalog.categories.api.CategoriesApi;
import ua.com.bravi.bravi.seller.controller.dto.in.CategoryCreateRequest;
import ua.com.bravi.bravi.seller.controller.dto.in.CategoryUpdateRequest;
import ua.com.bravi.bravi.seller.controller.dto.out.CategoryResponse;
import ua.com.bravi.bravi.seller.controller.mapper.CategoryDtoMapper;
import ua.com.bravi.bravi.shared.component.RequireStore;
import ua.com.bravi.bravi.seller.stores.api.CurrentStoreHolder;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/seller/categories")
@PreAuthorize("hasAuthority('role_seller')")
@Tag(name = "SellerCategoryController")
@RequireStore
public class SellerCategoryController {

    private final CategoriesApi categoriesApi;
    private final CategoryDtoMapper categoryDtoMapper;
    private final CurrentStoreHolder currentStoreHolder;

    @Operation(summary = "Get categories", description = "Returns the category tree of the current user's store")
    @GetMapping
    public List<CategoryResponse> getCategories() {
        return categoryDtoMapper.toResponses(categoriesApi.findTreeByStoreId(currentStoreHolder.get()));
    }

    @Operation(summary = "Get category", description = "Returns a category subtree of the current user's store")
    @GetMapping("/{categoryId}")
    public CategoryResponse getCategory(@PathVariable Long categoryId) {
        return categoryDtoMapper.toResponse(categoriesApi.getById(currentStoreHolder.get(), categoryId));
    }

    @Operation(summary = "Create category", description = "Creates a category in the current user's store")
    @PostMapping
    public ResponseEntity<Void> createCategory(@Valid @RequestBody CategoryCreateRequest request) {
        categoriesApi.create(currentStoreHolder.get(), categoryDtoMapper.toDomain(request));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Update category",
            description = "Partially updates a category; a non-null parent_id reparents it within the tree")
    @PatchMapping("/{categoryId}")
    public ResponseEntity<Void> updateCategory(
            @PathVariable Long categoryId,
            @Valid @RequestBody CategoryUpdateRequest request
    ) {
        categoriesApi.update(currentStoreHolder.get(), categoryId, categoryDtoMapper.toDomain(request));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Delete category",
            description = "Deletes a category of the current user's store; blocked if it has subcategories")
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long categoryId) {
        categoriesApi.delete(currentStoreHolder.get(), categoryId);
        return ResponseEntity.noContent().build();
    }
}
