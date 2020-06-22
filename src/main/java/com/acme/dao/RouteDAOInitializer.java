package com.acme.dao;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;

@AutoProtoSchemaBuilder(includeClasses = {RouteDAO.class}, schemaPackageName = "com.acme.dao")
interface RouteDAOInitializer extends SerializationContextInitializer {
}
