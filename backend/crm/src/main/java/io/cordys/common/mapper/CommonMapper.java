package io.cordys.common.mapper;

import org.apache.ibatis.annotations.Param;

public interface CommonMapper {
    boolean checkAddExist(@Param("tableName") String tableName,
                          @Param("fieldName") String fieldName,
                          @Param("fieldValue") String fieldValue,
                          @Param("orgId") String orgId);

    boolean checkUpdateExist(@Param("tableName") String tableName,
                             @Param("fieldName") String fieldName,
                             @Param("fieldValue") String fieldValue,
                             @Param("orgId") String orgId,
                             @Param("resource") Object resource);
}

