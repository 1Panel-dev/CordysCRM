package io.cordys.common.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;

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

    /**
     * 获取表属性值集合
     * @param tableName 表
     * @param fieldName 值
     * @param orgId 组织ID
     * @return 值集合
     */
    List<String> getValList(@Param("tableName") String tableName,
                            @Param("fieldName") String fieldName, @Param("orgId") String orgId);
}

