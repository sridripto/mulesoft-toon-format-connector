package com.github.toon.connector;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.toon.connector.internal.PayloadParser;
import com.github.toon.connector.internal.ToonConversionException;
import com.github.toon.connector.internal.ToonEncoder;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;

public class ToonOperations {

    private final PayloadParser parser = new PayloadParser();
    private final ToonEncoder encoder = new ToonEncoder();

    @MediaType(value = TEXT_PLAIN, strict = false)
    @DisplayName("Convert to TOON")
    @Summary("Converts a payload (JSON, XML, CSV, or String) into TOON format. Just pass #[payload] directly.")
    @Throws({ToonErrorTypeProvider.class})
    public String convertToToon(
            @DisplayName("Payload")
            @Summary("Pass #[payload] directly. Accepts JSON, XML, CSV, or plain String.")
            @Content
            InputStream payload,

            @DisplayName("Input Format")
            @Summary("Format of the input. Use AUTO to detect automatically.")
            @Optional(defaultValue = "AUTO")
            InputFormat inputFormat
    ) {
        try {
            String content = new String(payload.readAllBytes(), StandardCharsets.UTF_8);
            JsonNode parsed = parseByFormat(content, inputFormat);
            return encoder.encode(parsed);
        } catch (ToonConversionException e) {
            throw e;
        } catch (Exception e) {
            throw new ToonConversionException(
                    "Failed to convert payload to TOON: " + e.getMessage(),
                    ToonErrorType.ENCODING_ERROR,
                    e
            );
        }
    }

    private JsonNode parseByFormat(String content, InputFormat format) throws Exception {
        if (format == null) format = InputFormat.AUTO;
        switch (format) {
            case JSON:   return parser.parseJson(content);
            case XML:    return parser.parseXml(content);
            case CSV:    return parser.parseCsv(content);
            case STRING: return parser.parseString(content);
            case AUTO:
            default:     return parser.parseAuto(content);
        }
    }
}