package com.github.toon.connector;

import org.mule.runtime.extension.api.annotation.error.ErrorTypes;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

/**
 * Error types raised by the TOON Connector.
 *
 * <p>In Mule flows, you can catch these errors with an On Error handler:
 * <ul>
 *   <li>{@code TOON:PARSE_ERROR} — the input payload could not be parsed in the specified format</li>
 *   <li>{@code TOON:ENCODING_ERROR} — the parsed data could not be encoded to TOON</li>
 * </ul>
 */
public enum ToonErrorType implements ErrorTypeDefinition<ToonErrorType> {

    /**
     * Raised when the input payload cannot be parsed.
     * Possible causes: malformed JSON, invalid XML, invalid CSV.
     */
    PARSE_ERROR,

    /**
     * Raised when the parsed data structure cannot be encoded to TOON.
     * This should be rare and usually indicates an unsupported edge case.
     */
    ENCODING_ERROR
}