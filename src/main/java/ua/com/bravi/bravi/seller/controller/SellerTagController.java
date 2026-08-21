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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ua.com.bravi.bravi.seller.controller.dto.in.TagCreateRequest;
import ua.com.bravi.bravi.seller.controller.dto.in.TagMergeRequest;
import ua.com.bravi.bravi.seller.controller.dto.in.TagUpdateRequest;
import ua.com.bravi.bravi.seller.controller.dto.out.TagPageResponse;
import ua.com.bravi.bravi.seller.controller.dto.out.TagResponse;
import ua.com.bravi.bravi.seller.controller.mapper.TagDtoMapper;
import ua.com.bravi.bravi.seller.stores.api.StoreContext;
import ua.com.bravi.bravi.seller.tags.api.TagsApi;
import ua.com.bravi.bravi.seller.tags.domain.TagSearchQuery;
import ua.com.bravi.bravi.seller.tags.domain.TagSortBy;
import ua.com.bravi.bravi.seller.tags.domain.TagStatus;
import ua.com.bravi.bravi.seller.tags.domain.TagTarget;
import ua.com.bravi.bravi.shared.common.SortOrder;
import ua.com.bravi.bravi.shared.component.RequireStore;

import java.util.List;

/**
 * The tag dictionary. One dictionary serves every taggable aggregate, so the CRUD is written once
 * and told apart by the {@code target} path segment; the permission follows the target, which is
 * why these methods name it in the expression rather than spelling a fixed resource.
 *
 * <p>Pinning tags on things lives elsewhere, one controller per aggregate: proving that the thing
 * belongs to the store takes that aggregate's own API, and its routes belong beside the rest of it.
 *
 * <p>Never import the domain {@code Tag} here: the name is taken by the OpenAPI annotation below,
 * and Java has no import aliases. Nothing in this class needs to name the type.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/sellers/tags")
@Tag(name = "SellerTagController")
@RequireStore
public class SellerTagController {

    private final TagsApi tagsApi;
    private final TagDtoMapper tagDtoMapper;
    private final StoreContext storeContext;

    @Operation(summary = "Search tags",
            description = "Returns a paginated list of the store's tags for the given target "
                    + "(products or orders), each with the number of things it labels")
    @GetMapping("/{target}")
    @PreAuthorize("hasPermission(#target.resource(), 'READ')")
    public TagPageResponse searchTags(
            @PathVariable TagTarget target,
            @RequestParam(required = false) String search,
            @RequestParam(name = "statuses", required = false) List<TagStatus> statuses,
            @RequestParam(name = "sort_by", required = false) String sortBy,
            @RequestParam(name = "sort_order", defaultValue = "ASC") SortOrder sortOrder,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "limit", defaultValue = "20") int limit
    ) {
        TagSearchQuery query = new TagSearchQuery(search, statuses,
                sortBy == null ? null : TagSortBy.fromParam(sortBy), sortOrder, page, limit);
        return tagDtoMapper.toPageResponse(tagsApi.search(storeContext.get(), target, query));
    }

    @Operation(summary = "Get tag", description = "Returns a single tag of the given target")
    @GetMapping("/{target}/{publicId}")
    @PreAuthorize("hasPermission(#target.resource(), 'READ')")
    public TagResponse getTag(@PathVariable TagTarget target, @PathVariable String publicId) {
        return tagDtoMapper.toResponse(tagsApi.getByPublicId(storeContext.get(), target, publicId));
    }

    @Operation(summary = "Create tag",
            description = "Creates a tag explicitly. Tagging a thing with an unknown name also creates one, "
                    + "so this is for preparing the vocabulary up front")
    @PostMapping("/{target}")
    @PreAuthorize("hasPermission(#target.resource(), 'WRITE')")
    public ResponseEntity<TagResponse> createTag(@PathVariable TagTarget target,
                                                 @Valid @RequestBody TagCreateRequest request) {
        TagResponse response = tagDtoMapper.toResponse(
                tagsApi.create(storeContext.get(), target, tagDtoMapper.toDomain(request)));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Update tag",
            description = "Renames a tag or changes its status. Renaming onto a name the target already "
                    + "uses is refused; merge the two instead")
    @PatchMapping("/{target}/{publicId}")
    @PreAuthorize("hasPermission(#target.resource(), 'WRITE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateTag(@PathVariable TagTarget target, @PathVariable String publicId,
                          @Valid @RequestBody TagUpdateRequest request) {
        tagsApi.update(storeContext.get(), target, publicId, tagDtoMapper.toDomain(request));
    }

    @Operation(summary = "Delete tag",
            description = "Deletes the tag and unpins it from everything it labelled. The things themselves stay")
    @DeleteMapping("/{target}/{publicId}")
    @PreAuthorize("hasPermission(#target.resource(), 'WRITE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTag(@PathVariable TagTarget target, @PathVariable String publicId) {
        tagsApi.delete(storeContext.get(), target, publicId);
    }

    @Operation(summary = "Merge tags",
            description = "Moves everything the source tags label onto this one and deletes them")
    @PostMapping("/{target}/{publicId}/merge")
    @PreAuthorize("hasPermission(#target.resource(), 'WRITE')")
    public TagResponse mergeTags(@PathVariable TagTarget target, @PathVariable String publicId,
                                 @Valid @RequestBody TagMergeRequest request) {
        return tagDtoMapper.toResponse(
                tagsApi.merge(storeContext.get(), target, publicId, request.sourceIds()));
    }
}
