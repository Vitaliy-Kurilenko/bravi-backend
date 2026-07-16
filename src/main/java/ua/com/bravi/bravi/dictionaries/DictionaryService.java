package ua.com.bravi.bravi.dictionaries;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ua.com.bravi.bravi.dictionaries.api.DictionariesApi;
import ua.com.bravi.bravi.dictionaries.api.DictionaryItemView;
import ua.com.bravi.bravi.dictionaries.api.DictionaryView;
import ua.com.bravi.bravi.dictionaries.exception.DictionaryNotFoundException;
import ua.com.bravi.bravi.dictionaries.persistence.IDictionaryItemRepository;
import ua.com.bravi.bravi.dictionaries.persistence.IDictionaryRepository;
import ua.com.bravi.bravi.dictionaries.persistence.entity.DictionaryEntity;
import ua.com.bravi.bravi.dictionaries.persistence.mapper.DictionaryEntityMapper;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DictionaryService implements DictionariesApi {

    private final IDictionaryRepository dictionaryRepository;
    private final IDictionaryItemRepository dictionaryItemRepository;
    private final DictionaryEntityMapper dictionaryEntityMapper;

    @Override
    public List<DictionaryView> listDictionaries() {
        return dictionaryEntityMapper.toViews(dictionaryRepository.findAll(Sort.by("code")));
    }

    @Override
    public List<DictionaryItemView> listItems(String dictionaryCode) {
        DictionaryEntity dictionary = dictionaryRepository.findByCode(dictionaryCode)
                .orElseThrow(() -> new DictionaryNotFoundException(dictionaryCode));
        return dictionaryEntityMapper.toItemViews(
                dictionaryItemRepository.findByDictionaryIdAndActiveTrueOrderBySortOrderAscCodeAsc(dictionary.getId()));
    }

    @Override
    public boolean isActiveItem(String dictionaryCode, String itemCode) {
        return dictionaryItemRepository.existsByDictionary_CodeAndCodeAndActiveTrue(dictionaryCode, itemCode);
    }
}
