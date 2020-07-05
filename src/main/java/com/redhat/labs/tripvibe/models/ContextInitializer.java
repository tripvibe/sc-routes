package com.redhat.labs.tripvibe.models;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;

@AutoProtoSchemaBuilder(includeClasses = { Route.class, Direction.class, RouteDAO.class, RouteDirectionCacheKey.class, RouteType.class, RouteNameNumber.class, DirectionName.class, LatLongDistCacheKey.class , Stop.class, Stops.class}, schemaPackageName = "com.redhat.labs.tripvibe.models")
interface ContextInitializer extends SerializationContextInitializer {
}

