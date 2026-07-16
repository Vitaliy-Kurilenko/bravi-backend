package ua.com.bravi.bravi.dictionaries.exception;

public class DictionaryNotFoundException extends RuntimeException {

    public DictionaryNotFoundException(String code) {
        super("Dictionary '" + code + "' not found");
    }
}
