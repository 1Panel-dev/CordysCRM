package io.cordys.crm.integration.sqlbot.mapper;

import org.apache.ibatis.annotations.Param;


public interface ExtDataSourceMapper {
    String selectSchemaByDbName(@Param("dbName") String dbName);
}
