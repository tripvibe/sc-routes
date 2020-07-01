package com.redhat.labs.tripvibe.models;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;

@AutoProtoSchemaBuilder(includeClasses = { Route.class, Direction.class }, schemaPackageName = "com.redhat.labs.tripvibe.models")
interface ContextInitializer extends SerializationContextInitializer {
}
