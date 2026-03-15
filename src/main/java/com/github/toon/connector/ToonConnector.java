package com.github.toon.connector;

import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.runtime.extension.api.annotation.error.ErrorTypes;
import org.mule.sdk.api.annotation.JavaVersionSupport;
import org.mule.sdk.api.meta.JavaVersion;

@Extension(name = "TOON Connector")
@Operations(ToonOperations.class)
@ErrorTypes(ToonErrorType.class)
@Xml(prefix = "toon", namespace = "http://www.mulesoft.org/schema/mule/toon")
@JavaVersionSupport({JavaVersion.JAVA_17})
public class ToonConnector {
}