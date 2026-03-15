package com.github.toon.connector.internal;

import org.mule.runtime.extension.api.exception.ModuleException;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

/**
 * Custom exception thrown when the TOON connector fails to parse or encode a payload.
 */
public class ToonConversionException extends ModuleException {

    public ToonConversionException(String message, ErrorTypeDefinition<?> errorType, Throwable cause) {
        super(message, errorType, cause);
    }

    public ToonConversionException(String message, ErrorTypeDefinition<?> errorType) {
        super(message, errorType);
    }
}