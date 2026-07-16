package ua.com.bravi.bravi.dictionaries.persistence.mapper;

import org.mapstruct.Mapper;
import ua.com.bravi.bravi.dictionaries.api.DictionaryItemView;
import ua.com.bravi.bravi.dictionaries.api.DictionaryView;
import ua.com.bravi.bravi.dictionaries.persistence.entity.DictionaryEntity;
import ua.com.bravi.bravi.dictionaries.persistence.entity.DictionaryItemEntity;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DictionaryEntityMapper {

    DictionaryView toView(DictionaryEntity entity);

    List<DictionaryView> toViews(List<DictionaryEntity> entities);

    DictionaryItemView toItemView(DictionaryItemEntity entity);

    List<DictionaryItemView> toItemViews(List<DictionaryItemEntity> entities);
}
