package com.github.toon.connector;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.toon.connector.internal.PayloadParser;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for the {@link PayloadParser}.
 */
public class PayloadParserTest {

    private final PayloadParser parser = new PayloadParser();

    // -----------------------------------------------------------------------
    // JSON parsing
    // -----------------------------------------------------------------------

    @Test
    public void testParseJsonObject() throws Exception {
        JsonNode node = parser.parseJson("{\"name\":\"Alice\",\"age\":30}");
        assertTrue("Should be object", node.isObject());
        assertEquals("Alice", node.get("name").asText());
        assertEquals(30, node.get("age").asInt());
    }

    @Test
    public void testParseJsonArray() throws Exception {
        JsonNode node = parser.parseJson("[1,2,3]");
        assertTrue("Should be array", node.isArray());
        assertEquals(3, node.size());
    }

    // -----------------------------------------------------------------------
    // XML parsing
    // -----------------------------------------------------------------------

    @Test
    public void testParseXmlSimple() throws Exception {
        String xml = "<user><n>Alice</n><role>admin</role></user>";
        JsonNode node = parser.parseXml(xml);
        assertTrue("Should be object with root 'user'", node.has("user"));
        JsonNode user = node.get("user");
        assertEquals("Alice", user.get("n").asText());
        assertEquals("admin", user.get("role").asText());
    }

    @Test
    public void testParseXmlAttribute() throws Exception {
        String xml = "<user id=\"42\"><n>Alice</n></user>";
        JsonNode node = parser.parseXml(xml);
        JsonNode user = node.get("user");
        assertTrue("Should have @id attribute", user.has("@id"));
        assertEquals("42", user.get("@id").asText());
    }

    @Test
    public void testParseXmlRepeatingChildren() throws Exception {
        String xml = "<items><item>A</item><item>B</item><item>C</item></items>";
        JsonNode node = parser.parseXml(xml);
        JsonNode items = node.get("items");
        assertTrue("Repeated elements should be array", items.get("item").isArray());
        assertEquals(3, items.get("item").size());
    }

    // -----------------------------------------------------------------------
    // CSV parsing
    // -----------------------------------------------------------------------

    @Test
    public void testParseCsv() throws Exception {
        String csv = "id,name,role\n1,Alice,admin\n2,Bob,user";
        JsonNode node = parser.parseCsv(csv);
        assertTrue("Should be array", node.isArray());
        assertEquals(2, node.size());
        assertEquals(1, node.get(0).get("id").asInt());
        assertEquals("Alice", node.get(0).get("name").asText());
    }

    @Test
    public void testCsvTypeInference() throws Exception {
        String csv = "id,active,score\n1,true,9.5\n2,false,7.0";
        JsonNode node = parser.parseCsv(csv);
        assertTrue("id should be int", node.get(0).get("id").isInt());
        assertTrue("active should be boolean", node.get(0).get("active").isBoolean());
        assertTrue("score should be number", node.get(0).get("score").isNumber());
    }

    // -----------------------------------------------------------------------
    // Auto detection
    // -----------------------------------------------------------------------

    @Test
    public void testAutoDetectJson() throws Exception {
        JsonNode node = parser.parseAuto("{\"hello\":\"world\"}");
        assertTrue("Should detect JSON", node.isObject());
        assertEquals("world", node.get("hello").asText());
    }

    @Test
    public void testAutoDetectXml() throws Exception {
        JsonNode node = parser.parseAuto("<root><value>42</value></root>");
        assertTrue("Should detect XML and wrap in root object", node.has("root"));
    }

    @Test
    public void testAutoDetectCsv() throws Exception {
        JsonNode node = parser.parseAuto("a,b,c\n1,2,3\n4,5,6");
        assertTrue("Should detect CSV as array", node.isArray());
        assertEquals(2, node.size());
    }

    @Test
    public void testAutoDetectString() throws Exception {
        JsonNode node = parser.parseAuto("just a plain string");
        assertTrue("Should fall back to text node", node.isTextual());
        assertEquals("just a plain string", node.asText());
    }
}