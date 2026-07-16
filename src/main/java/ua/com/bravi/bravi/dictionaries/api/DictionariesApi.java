package ua.com.bravi.bravi.dictionaries.api;

import java.util.List;

public interface DictionariesApi {

    List<DictionaryView> listDictionaries();

    /**
     * Active items of the dictionary, ordered by sort order.
     *
     * @throws ua.com.bravi.bravi.dictionaries.exception.DictionaryNotFoundException for an unknown dictionary code
     */
    List<DictionaryItemView> listItems(String dictionaryCode);

    /**
     * True when the dictionary exists and contains an active item with the given code; false otherwise.
     */
    boolean isActiveItem(String dictionaryCode, String itemCode);
}
