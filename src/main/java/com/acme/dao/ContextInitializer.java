package com.acme.dao;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;

@AutoProtoSchemaBuilder(includeClasses = { com.acme.dao.RouteDAO.class, com.acme.dao.CacheKey.class, com.acme.dao.RouteType.class, com.acme.dao.RouteNameNumber.class, com.acme.dao.DirectionName.class}, schemaPackageName = "com.acme.dao")
interface ContextInitializer extends SerializationContextInitializer {
}
