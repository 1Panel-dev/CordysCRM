package cn.cordys.common.mapper;

import cn.cordys.common.domain.BaseResourceSubField;
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
                             @Param("excludeIds") List<String> excludeIds);

    /**
     * 获取表属性值集合
     *
     * @param tableName 表
     * @param fieldName 值
     * @param orgId     组织ID
     * @return 值集合
     */
    List<BaseResourceSubField> getCheckValList(@Param("tableName") String tableName,
                                 @Param("fieldName") String fieldName, @Param("orgId") String orgId);

    /**
     * 校验字段值是否重复
     *
     * @param dataTable  数据表
     * @param fieldTable 字段表
     * @param fieldId    字段ID
     * @param fieldValue 字段值
     * @param orgId      组织ID
     * @return 是否重复
     */
    String checkFieldRepeatName(@Param("dataTable") String dataTable, @Param("fieldTable") String fieldTable,
                                @Param("fieldId") String fieldId, @Param("fieldValue") String fieldValue, @Param("orgId") String orgId);

    List<BaseResourceSubField> getCheckFieldValList(@Param("dataTable") String dataTable, @Param("fieldTable") String fieldTable,
                                      @Param("fieldId") String fieldId, @Param("orgId") String orgId);

    /**
     * 校验业务字段是否重复
     *
     * @param dataTable    数据表
     * @param businessName 业务字段名
     * @param value        值
     * @param orgId        组织ID
     * @return 是否重复
     */
    String checkInternalRepeatName(@Param("dataTable") String dataTable, @Param("businessName") String businessName,
                                   @Param("value") String value, @Param("orgId") String orgId);

    /**
     * 获取资源自定义字段
     *
     * @param tableName
     * @param resourceId
     * @param fieldId
     * @return
     */
    BaseResourceSubField getResourceField(@Param("tableName") String tableName, @Param("resourceId") String resourceId, @Param("fieldId") String fieldId);

    /**
     * 更新自定义字段
     *
     * @param tableName
     * @param field
     */
    void updateCustomerField(@Param("tableName") String tableName, @Param("field") BaseResourceSubField field);

    int checkIdCount(@Param("id")String id, @Param("tableName")String tableName);
}

