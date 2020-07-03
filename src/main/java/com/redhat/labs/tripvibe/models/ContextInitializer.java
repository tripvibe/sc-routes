package com.redhat.labs.tripvibe.models;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;

@AutoProtoSchemaBuilder(includeClasses = { Route.class, Direction.class, RouteDAO.class, CacheKey.class, RouteType.class, RouteNameNumber.class, DirectionName.class }, schemaPackageName = "com.redhat.labs.tripvibe.models")
interface ContextInitializer extends SerializationContextInitializer {
}
