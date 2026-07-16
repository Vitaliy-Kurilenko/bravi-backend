package ua.com.bravi.bravi.dictionaries.controller.mapper;

import org.mapstruct.Mapper;
import ua.com.bravi.bravi.dictionaries.api.DictionaryItemView;
import ua.com.bravi.bravi.dictionaries.api.DictionaryView;
import ua.com.bravi.bravi.dictionaries.controller.dto.out.DictionaryItemResponse;
import ua.com.bravi.bravi.dictionaries.controller.dto.out.DictionaryResponse;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DictionaryDtoMapper {

    DictionaryResponse toResponse(DictionaryView view);

    List<DictionaryResponse> toResponses(List<DictionaryView> views);

    DictionaryItemResponse toItemResponse(DictionaryItemView view);

    List<DictionaryItemResponse> toItemResponses(List<DictionaryItemView> views);
}
