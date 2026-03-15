# mulesoft-toon-format-connector



Converts Mule payloads (JSON, XML, CSV, String) to TOON (Token-Oriented Object Notation) — a compact, token-efficient format for LLM input.



\## Requirements

\- Mule Runtime 4.6.0+

\- Java 17

\- Anypoint Studio 7.x



\## Installation



Add to your Mule project's `pom.xml`:

```xml

<dependency>

&#x20;   <groupId>com.github.toon</groupId>

&#x20;   <artifactId>mule-toon-connector</artifactId>

&#x20;   <version>1.0.0</version>

&#x20;   <classifier>mule-plugin</classifier>

</dependency>

```



\## Build from source

```cmd

git clone https://github.com/sridripto/mule-toon-connector.git

cd mule-toon-connector

mvn clean install

```

