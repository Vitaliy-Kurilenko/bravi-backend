package ua.com.bravi.bravi.dictionaries.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.com.bravi.bravi.dictionaries.api.DictionariesApi;
import ua.com.bravi.bravi.dictionaries.controller.dto.out.DictionaryItemResponse;
import ua.com.bravi.bravi.dictionaries.controller.dto.out.DictionaryResponse;
import ua.com.bravi.bravi.dictionaries.controller.mapper.DictionaryDtoMapper;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/dictionaries")
@Tag(name = "DictionaryController")
public class DictionaryController {

    private final DictionariesApi dictionariesApi;
    private final DictionaryDtoMapper dictionaryDtoMapper;

    @Operation(summary = "List dictionaries",
            description = "Returns all system reference dictionaries (code + name)")
    @GetMapping
    public List<DictionaryResponse> getDictionaries() {
        return dictionaryDtoMapper.toResponses(dictionariesApi.listDictionaries());
    }

    @Operation(summary = "List dictionary items",
            description = "Returns active items of the dictionary ordered by sort order; "
                    + "dictionary codes are case-sensitive (e.g. CURRENCY)")
    @GetMapping("/{code}")
    public List<DictionaryItemResponse> getDictionaryItems(@PathVariable String code) {
        return dictionaryDtoMapper.toItemResponses(dictionariesApi.listItems(code));
    }
}
