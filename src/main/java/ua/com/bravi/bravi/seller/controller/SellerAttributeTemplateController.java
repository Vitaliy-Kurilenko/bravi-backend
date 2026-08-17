package ua.com.bravi.bravi.seller.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ua.com.bravi.bravi.seller.catalog.attributes.api.AttributesApi;
import ua.com.bravi.bravi.seller.controller.dto.out.AttributeTemplateResponse;
import ua.com.bravi.bravi.seller.controller.mapper.AttributeDtoMapper;
import ua.com.bravi.bravi.seller.stores.api.StoreContext;
import ua.com.bravi.bravi.shared.component.RequireStore;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/sellers/attribute-templates")
@Tag(name = "SellerAttributeTemplateController")
@RequireStore
public class SellerAttributeTemplateController {

    private final AttributesApi attributesApi;
    private final AttributeDtoMapper attributeDtoMapper;
    private final StoreContext storeContext;

    @Operation(summary = "List attribute templates",
            description = "Returns the shared library of common attributes, flagging the ones the current "
                    + "store has already adopted. A template is adopted by naming its code when binding "
                    + "attributes to a category.")
    @GetMapping
    @PreAuthorize("hasPermission('PRODUCT', 'READ')")
    public List<AttributeTemplateResponse> getAttributeTemplates(@RequestParam(required = false) String search) {
        return attributeDtoMapper.toTemplateResponses(attributesApi.listTemplates(storeContext.get(), search));
    }
}
