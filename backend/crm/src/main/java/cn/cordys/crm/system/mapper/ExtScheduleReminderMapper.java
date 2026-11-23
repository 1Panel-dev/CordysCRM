package cn.cordys.crm.system.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 定时提醒通用取数
 * <p>
 * 表名/列名均为服务端常量传入（安全的字符串插值），字段值/组织/表单/时间区间为绑定参数。
 * 返回 {@code select t.*} 的行，调用方按需从 map 中读取 name/owner/create_user/organization_id 等列。
 * </p>
 */
public interface ExtScheduleReminderMapper {

    /**
     * 按主表内置时间列查询命中记录
     */
    List<Map<String, Object>> selectRowsByColumn(@Param("mainTable") String mainTable,
            @Param("column") String column,
            @Param("orgId") String orgId,
            @Param("start") long start,
            @Param("end") long end,
            @Param("formColumn") String formColumn,
            @Param("formValue") String formValue);

    /**
     * 按自定义日期字段查询命中记录（join ${fieldTable}，field_value 存毫秒字符串）
     */
    List<Map<String, Object>> selectRowsByCustomField(@Param("mainTable") String mainTable,
            @Param("fieldTable") String fieldTable,
            @Param("fieldId") String fieldId,
            @Param("orgId") String orgId,
            @Param("start") long start,
            @Param("end") long end,
            @Param("formColumn") String formColumn,
            @Param("formValue") String formValue);
}
