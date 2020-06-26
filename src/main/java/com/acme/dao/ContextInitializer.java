package com.acme.dao;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;

@AutoProtoSchemaBuilder(includeClasses = {RouteDAO.class, CacheKey.class}, schemaPackageName = "com.acme.dao")
interface ContextInitializer extends SerializationContextInitializer {
}
