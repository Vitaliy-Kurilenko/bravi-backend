package ua.com.bravi.bravi.dictionaries.controller.dto.out;

import java.util.Map;

public record DictionaryItemResponse(
        String code,
        String name,
        Map<String, Object> meta
) {
}
