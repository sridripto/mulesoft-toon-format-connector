package com.github.toon.connector;

/**
 * Enumeration of supported input payload formats for TOON conversion.
 */
public enum InputFormat {

    /**
     * Automatically detect the format from the payload content.
     * The connector will try JSON → XML → CSV → plain String in that order.
     */
    AUTO,

    /**
     * Parse the input as a JSON string (objects and arrays).
     */
    JSON,

    /**
     * Parse the input as an XML document.
     */
    XML,

    /**
     * Parse the input as a CSV (comma-separated values) document.
     * The first row is treated as the header row (field names).
     */
    CSV,

    /**
     * Treat the input as a plain string (scalar value).
     * This simply wraps the value in TOON scalar notation.
     */
    STRING
}