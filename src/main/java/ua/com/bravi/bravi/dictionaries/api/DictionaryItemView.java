package ua.com.bravi.bravi.dictionaries.api;

import java.util.Map;

public record DictionaryItemView(
        Long id,
        String code,
        String name,
        Integer sortOrder,
        Map<String, Object> meta
) {
}
