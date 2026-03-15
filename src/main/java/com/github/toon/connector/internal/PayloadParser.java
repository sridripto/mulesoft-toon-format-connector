package com.github.toon.connector.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Utility class that parses various input formats into a Jackson {@link JsonNode} tree,
 * which is then fed to the {@link ToonEncoder}.
 *
 * <p>Supported formats: JSON, XML, CSV, and plain String.
 * Auto-detection tries each parser in order: JSON → XML → CSV → String.
 */
public class PayloadParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // -----------------------------------------------------------------------
    // Public entry points
    // -----------------------------------------------------------------------

    /**
     * Parse a JSON string into a JsonNode.
     *
     * @param input the JSON-formatted string
     * @return the parsed JsonNode
     * @throws Exception if parsing fails
     */
    public JsonNode parseJson(String input) throws Exception {
        if (input == null || input.trim().isEmpty()) {
            return MAPPER.getNodeFactory().nullNode();
        }
        return MAPPER.readTree(input.trim());
    }

    /**
     * Parse an XML string into a JsonNode.
     * XML attributes and text content are represented as object fields.
     * Child elements with the same tag name are collapsed into arrays.
     *
     * @param input the XML-formatted string
     * @return the parsed JsonNode
     * @throws Exception if parsing fails
     */
    public JsonNode parseXml(String input) throws Exception {
        if (input == null || input.trim().isEmpty()) {
            return MAPPER.getNodeFactory().nullNode();
        }
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // Security: disable external entity processing
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(
                new ByteArrayInputStream(input.trim().getBytes(StandardCharsets.UTF_8)));
        doc.getDocumentElement().normalize();
        Element root = doc.getDocumentElement();
        ObjectNode rootNode = MAPPER.createObjectNode();
        rootNode.set(root.getTagName(), xmlElementToJson(root));
        return rootNode;
    }

    /**
     * Parse a CSV string into a JsonNode (an array of objects).
     * The first row is used as the header (field names).
     *
     * @param input the CSV-formatted string
     * @return an ArrayNode where each element is an object for one row
     * @throws Exception if parsing fails
     */
    public JsonNode parseCsv(String input) throws Exception {
        if (input == null || input.trim().isEmpty()) {
            return MAPPER.createArrayNode();
        }
        ArrayNode arrayNode = MAPPER.createArrayNode();
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true)
                .setTrim(true)
                .build();

        try (CSVParser parser = CSVParser.parse(new StringReader(input), format)) {
            List<String> headers = parser.getHeaderNames();
            for (CSVRecord record : parser) {
                ObjectNode row = MAPPER.createObjectNode();
                for (String header : headers) {
                    String value = record.get(header);
                    // Attempt to coerce numbers and booleans
                    row.set(header, inferType(value));
                }
                arrayNode.add(row);
            }
        }
        return arrayNode;
    }

    /**
     * Wrap a plain string as a TOON scalar.
     *
     * @param input the plain string
     * @return a TextNode
     */
    public JsonNode parseString(String input) {
        if (input == null) {
            return MAPPER.getNodeFactory().nullNode();
        }
        return MAPPER.getNodeFactory().textNode(input);
    }

    /**
     * Auto-detect the format and parse. Tries JSON → XML → CSV → String.
     *
     * @param input the raw input string
     * @return the parsed JsonNode
     */
    public JsonNode parseAuto(String input) {
        if (input == null || input.trim().isEmpty()) {
            return MAPPER.getNodeFactory().nullNode();
        }
        String trimmed = input.trim();

        // Try JSON
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            try {
                return parseJson(trimmed);
            } catch (Exception ignored) { /* fall through */ }
        }

        // Try XML
        if (trimmed.startsWith("<")) {
            try {
                return parseXml(trimmed);
            } catch (Exception ignored) { /* fall through */ }
        }

        // Try CSV (must have at least one comma and two lines)
        if (trimmed.contains(",") && trimmed.contains("\n")) {
            try {
                JsonNode csvResult = parseCsv(trimmed);
                if (csvResult.isArray() && csvResult.size() > 0) {
                    return csvResult;
                }
            } catch (Exception ignored) { /* fall through */ }
        }

        // Default: plain string
        return parseString(input);
    }

    // -----------------------------------------------------------------------
    // Internal XML helpers
    // -----------------------------------------------------------------------

    private JsonNode xmlElementToJson(Element element) {
        ObjectNode node = MAPPER.createObjectNode();

        // Add XML attributes as @attributeName fields
        if (element.hasAttributes()) {
            for (int i = 0; i < element.getAttributes().getLength(); i++) {
                Node attr = element.getAttributes().item(i);
                node.put("@" + attr.getNodeName(), attr.getNodeValue());
            }
        }

        NodeList children = element.getChildNodes();
        boolean hasElementChildren = false;

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                hasElementChildren = true;
                String childName = child.getNodeName();
                JsonNode childJson = xmlElementToJson((Element) child);

                if (node.has(childName)) {
                    // Already exists — convert to array
                    JsonNode existing = node.get(childName);
                    ArrayNode arr;
                    if (existing.isArray()) {
                        arr = (ArrayNode) existing;
                    } else {
                        arr = MAPPER.createArrayNode();
                        arr.add(existing);
                        node.set(childName, arr);
                    }
                    arr.add(childJson);
                } else {
                    node.set(childName, childJson);
                }
            }
        }

        // If no element children, extract text content
        if (!hasElementChildren) {
            String textContent = element.getTextContent().trim();
            if (!textContent.isEmpty()) {
                if (node.isEmpty()) {
                    // Pure text node — return as scalar
                    return inferType(textContent);
                } else {
                    node.set("#text", inferType(textContent));
                }
            }
        }

        return node.isEmpty() ? MAPPER.getNodeFactory().nullNode() : node;
    }

    // -----------------------------------------------------------------------
    // Type inference for CSV and XML text values
    // -----------------------------------------------------------------------

    /**
     * Attempt to coerce a string value to a more specific JSON type.
     * Tries integer, double, boolean, then falls back to string.
     */
    private JsonNode inferType(String value) {
        if (value == null || value.isEmpty()) {
            return MAPPER.getNodeFactory().textNode(value != null ? value : "");
        }

        // Boolean
        if (value.equalsIgnoreCase("true"))  return MAPPER.getNodeFactory().booleanNode(true);
        if (value.equalsIgnoreCase("false")) return MAPPER.getNodeFactory().booleanNode(false);

        // Null keyword
        if (value.equalsIgnoreCase("null")) return MAPPER.getNodeFactory().nullNode();

        // Integer
        try {
            long longVal = Long.parseLong(value);
            if (longVal >= Integer.MIN_VALUE && longVal <= Integer.MAX_VALUE) {
                return MAPPER.getNodeFactory().numberNode((int) longVal);
            }
            return MAPPER.getNodeFactory().numberNode(longVal);
        } catch (NumberFormatException ignored) { /* not integer */ }

        // Double
        try {
            double doubleVal = Double.parseDouble(value);
            return MAPPER.getNodeFactory().numberNode(doubleVal);
        } catch (NumberFormatException ignored) { /* not double */ }

        // Default: string
        return MAPPER.getNodeFactory().textNode(value);
    }
}