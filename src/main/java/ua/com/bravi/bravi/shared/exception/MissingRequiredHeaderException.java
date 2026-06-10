package ua.com.bravi.bravi.shared.exception;

import lombok.Getter;

@Getter
public class MissingRequiredHeaderException extends RuntimeException {

    private final String headerName;

    public MissingRequiredHeaderException(String headerName) {
        super("Required header is missing: " + headerName);
        this.headerName = headerName;
    }
}
