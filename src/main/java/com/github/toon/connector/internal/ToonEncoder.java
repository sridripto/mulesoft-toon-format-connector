package com.github.toon.connector.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Core TOON encoder.
 */
public class ToonEncoder {

    private static final String INDENT = "  ";
    private static final int TABULAR_THRESHOLD = 2;

    public String encode(JsonNode node) {
        StringBuilder sb = new StringBuilder();
        encodeNode(node, sb, 0, null);
        String result = sb.toString();
        int end = result.length();
        while (end > 0 && Character.isWhitespace(result.charAt(end - 1))) end--;
        return result.substring(0, end);
    }

    private void encodeNode(JsonNode node, StringBuilder sb, int depth, String key) {
        if (node.isObject()) {
            encodeObject((ObjectNode) node, sb, depth, key);
        } else if (node.isArray()) {
            encodeArray((ArrayNode) node, sb, depth, key);
        } else {
            String scalar = encodeScalar(node);
            if (key != null) {
                sb.append(indent(depth)).append(key).append(": ").append(scalar).append("\n");
            } else {
                sb.append(scalar).append("\n");
            }
        }
    }

    private void encodeObject(ObjectNode node, StringBuilder sb, int depth, String key) {
        if (key != null) {
            sb.append(indent(depth)).append(key).append(":\n");
        }
        int childDepth = (key != null) ? depth + 1 : depth;
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            encodeNode(entry.getValue(), sb, childDepth, entry.getKey());
        }
    }

    private void encodeArray(ArrayNode array, StringBuilder sb, int depth, String key) {
        int size = array.size();
        if (size == 0) {
            String keyPart = (key != null) ? key : "";
            sb.append(indent(depth)).append(keyPart).append("[0]: \n");
            return;
        }
        if (isAllScalars(array)) {
            encodeScalarArray(array, sb, depth, key, size);
            return;
        }
        List<String> commonFields = getUniformObjectFields(array);
        if (commonFields != null && size >= TABULAR_THRESHOLD) {
            encodeTabularArray(array, sb, depth, key, size, commonFields);
            return;
        }
        encodeExpandedArray(array, sb, depth, key, size);
    }

    private void encodeScalarArray(ArrayNode array, StringBuilder sb, int depth, String key, int size) {
        StringBuilder values = new StringBuilder();
        for (int i = 0; i < size; i++) {
            if (i > 0) values.append(",");
            values.append(encodeScalar(array.get(i)));
        }
        String keyPart = (key != null) ? key : "";
        sb.append(indent(depth)).append(keyPart).append("[").append(size).append("]: ")
                .append(values).append("\n");
    }

    private void encodeTabularArray(ArrayNode array, StringBuilder sb, int depth, String key,
                                     int size, List<String> fields) {
        String keyPart = (key != null) ? key : "";
        sb.append(indent(depth)).append(keyPart)
                .append("[").append(size).append(",]")
                .append("{").append(String.join(",", fields)).append("}:\n");
        String rowIndent = indent(depth + 1);
        for (JsonNode element : array) {
            StringBuilder row = new StringBuilder(rowIndent);
            for (int i = 0; i < fields.size(); i++) {
                if (i > 0) row.append(",");
                JsonNode fieldValue = element.get(fields.get(i));
                row.append(fieldValue != null ? encodeScalar(fieldValue) : "null");
            }
            sb.append(row).append("\n");
        }
    }

    private void encodeExpandedArray(ArrayNode array, StringBuilder sb, int depth, String key, int size) {
        String keyPart = (key != null) ? key : "";
        sb.append(indent(depth)).append(keyPart).append("[").append(size).append("]:\n");
        int childDepth = depth + 1;
        for (JsonNode element : array) {
            if (element.isObject()) {
                encodeObject((ObjectNode) element, sb, childDepth, null);
                if (size > 1) sb.append(indent(childDepth)).append("---\n");
            } else if (element.isArray()) {
                encodeArray((ArrayNode) element, sb, childDepth, null);
            } else {
                sb.append(indent(childDepth)).append(encodeScalar(element)).append("\n");
            }
        }
    }

    String encodeScalar(JsonNode node) {
        if (node.isNull()) return "null";
        if (node.isBoolean()) return node.asBoolean() ? "true" : "false";
        if (node.isNumber()) {
            if (node.isFloatingPointNumber()) {
                double d = node.asDouble();
                if (Double.isNaN(d) || Double.isInfinite(d)) return "null";
                return node.decimalValue() != null
                        ? node.decimalValue().toPlainString()
                        : String.valueOf(d);
            }
            return node.asText();
        }
        String text = node.asText();
        return needsQuoting(text) ? "\"" + escapeString(text) + "\"" : text;
    }

    private boolean needsQuoting(String text) {
        if (text == null || text.isEmpty()) return true;
        if (text.startsWith(" ") || text.endsWith(" ")) return true;
        if (text.contains(",") || text.contains(":") || text.contains("\n")
                || text.contains("\r") || text.contains("{") || text.contains("}")
                || text.contains("[") || text.contains("]")) return true;
        if (text.equals("null") || text.equals("true") || text.equals("false")) return true;
        return false;
    }

    private String escapeString(String text) {
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    private String indent(int depth) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < depth; i++) sb.append(INDENT);
        return sb.toString();
    }

    private boolean isAllScalars(ArrayNode array) {
        for (JsonNode node : array) {
            if (node.isObject() || node.isArray()) return false;
        }
        return true;
    }

    private List<String> getUniformObjectFields(ArrayNode array) {
        List<String> referenceFields = null;
        for (JsonNode node : array) {
            if (!node.isObject()) return null;
            List<String> nodeFields = new ArrayList<>();
            Iterator<String> fieldNames = node.fieldNames();
            while (fieldNames.hasNext()) nodeFields.add(fieldNames.next());
            if (referenceFields == null) {
                referenceFields = nodeFields;
            } else {
                if (!referenceFields.equals(nodeFields)) return null;
            }
            for (String fieldName : nodeFields) {
                JsonNode fieldValue = node.get(fieldName);
                if (fieldValue != null && (fieldValue.isObject() || fieldValue.isArray())) return null;
            }
        }
        return referenceFields;
    }
}