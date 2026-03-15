package com.github.toon.connector;

import org.mule.runtime.extension.api.annotation.error.ErrorTypeProvider;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

import java.util.Set;

/**
 * Declares the error types that the TOON connector operations can throw.
 * This class is referenced by the {@code @Throws} annotation on each operation.
 */
public class ToonErrorTypeProvider implements ErrorTypeProvider {

    @Override
    public Set<ErrorTypeDefinition> getErrorTypes() {
        java.util.HashSet<ErrorTypeDefinition> errors = new java.util.HashSet<>();
        errors.add(ToonErrorType.PARSE_ERROR);
        errors.add(ToonErrorType.ENCODING_ERROR);
        return errors;
    }
}