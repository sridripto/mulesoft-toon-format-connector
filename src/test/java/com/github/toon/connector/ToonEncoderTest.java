package com.github.toon.connector;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.toon.connector.internal.ToonEncoder;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for the {@link ToonEncoder}.
 *
 * <p>These tests verify the TOON encoding rules against the official spec examples.
 */
public class ToonEncoderTest {

    private final ToonEncoder encoder = new ToonEncoder();
    private final ObjectMapper mapper = new ObjectMapper();

    // -----------------------------------------------------------------------
    // Simple object
    // -----------------------------------------------------------------------

    @Test
    public void testSimpleObject() throws Exception {
        String json = "{\"name\": \"Alice\", \"age\": 30}";
        String result = encoder.encode(mapper.readTree(json));
        assertTrue("Should contain 'name: Alice'", result.contains("name: Alice"));
        assertTrue("Should contain 'age: 30'", result.contains("age: 30"));
    }

    // -----------------------------------------------------------------------
    // Scalar array
    // -----------------------------------------------------------------------

    @Test
    public void testScalarArray() throws Exception {
        String json = "[\"alpha\", \"beta\", \"gamma\"]";
        String result = encoder.encode(mapper.readTree(json));
        // Should be: [3]: alpha,beta,gamma
        assertTrue("Should use tabular scalar array syntax", result.contains("[3]:"));
        assertTrue("Should contain all values", result.contains("alpha") && result.contains("beta"));
    }

    // -----------------------------------------------------------------------
    // Uniform object array → tabular
    // -----------------------------------------------------------------------

    @Test
    public void testUniformObjectArrayTabular() throws Exception {
        String json = "[" +
                "{\"id\":1,\"name\":\"Alice\",\"role\":\"admin\"}," +
                "{\"id\":2,\"name\":\"Bob\",\"role\":\"user\"}" +
                "]";
        String result = encoder.encode(mapper.readTree(json));
        // Should use tabular format: [2,]{id,name,role}:
        assertTrue("Should use tabular array header", result.contains("[2,]{id,name,role}:"));
        assertTrue("Should contain row 1,Alice,admin", result.contains("1,Alice,admin"));
        assertTrue("Should contain row 2,Bob,user", result.contains("2,Bob,user"));
    }

    // -----------------------------------------------------------------------
    // Nested object
    // -----------------------------------------------------------------------

    @Test
    public void testNestedObject() throws Exception {
        String json = "{\"user\": {\"name\": \"Alice\", \"address\": {\"city\": \"Kolkata\"}}}";
        String result = encoder.encode(mapper.readTree(json));
        assertTrue("Should contain 'user:'", result.contains("user:"));
        assertTrue("Should contain 'name: Alice'", result.contains("name: Alice"));
        assertTrue("Should contain 'city: Kolkata'", result.contains("city: Kolkata"));
    }

    // -----------------------------------------------------------------------
    // Boolean and null values
    // -----------------------------------------------------------------------

    @Test
    public void testBooleanAndNull() throws Exception {
        String json = "{\"active\": true, \"deleted\": false, \"notes\": null}";
        String result = encoder.encode(mapper.readTree(json));
        assertTrue("Should encode true", result.contains("active: true"));
        assertTrue("Should encode false", result.contains("deleted: false"));
        assertTrue("Should encode null", result.contains("notes: null"));
    }

    // -----------------------------------------------------------------------
    // String quoting
    // -----------------------------------------------------------------------

    @Test
    public void testStringQuoting() throws Exception {
        String json = "{\"value\": \"hello, world\"}";
        String result = encoder.encode(mapper.readTree(json));
        // Comma in value should trigger quoting
        assertTrue("Comma in string should be quoted", result.contains("\"hello, world\""));
    }

    @Test
    public void testSimpleStringNoQuoting() throws Exception {
        String json = "{\"city\": \"Kolkata\"}";
        String result = encoder.encode(mapper.readTree(json));
        // No special chars — should NOT be quoted
        assertFalse("Simple string should not be quoted", result.contains("\"Kolkata\""));
        assertTrue("Should contain unquoted city", result.contains("city: Kolkata"));
    }

    // -----------------------------------------------------------------------
    // Empty object/array
    // -----------------------------------------------------------------------

    @Test
    public void testEmptyArray() throws Exception {
        String json = "[]";
        String result = encoder.encode(mapper.readTree(json));
        assertTrue("Empty array should show [0]", result.contains("[0]"));
    }

    @Test
    public void testEmptyObject() throws Exception {
        String json = "{}";
        String result = encoder.encode(mapper.readTree(json));
        // Empty object produces empty output
        assertNotNull(result);
    }
}