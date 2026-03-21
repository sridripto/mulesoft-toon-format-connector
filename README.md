# MuleSoft TOON Format Converter Connector

Converts Mule payloads (JSON, XML, CSV, String) to TOON (Token-Oriented Object Notation) — a compact, token-efficient format for LLM input.

## Requirements
- Mule Runtime 4.6.0+
- Java 17
- Anypoint Studio 7.x

## Build from source

```cmd

mvn clean install

```

## Installation

Add to your Mule project's `pom.xml`:

```xml

<dependency>
	<groupId>com.github.toon</groupId>
	<artifactId>mule-toon-connector</artifactId>
	<version>1.0.0</version>
	<classifier>mule-plugin</classifier>
</dependency>

```
