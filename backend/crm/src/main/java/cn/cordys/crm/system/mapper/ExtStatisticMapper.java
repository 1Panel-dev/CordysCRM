package cn.cordys.crm.system.mapper;

import cn.cordys.crm.system.dto.StatisticFieldSourceDTO;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * 统计字段刷新用的通用 SQL: 字段配置反查 + 字段值读写。
 *
 * <p>这里只放「与表单无关」的语句 —— 表名、列名全是调用方传进来的参数, 不依赖任何一张表单的字段定义。
 * 曾经的筛选与聚合也在这里, 用一套动态表名的通用片段渲染所有表单; 那个前提是错的:
 * 同一个条件名在不同表单上落到哪个列只有那张表单的 Mapper 知道
 * (「products」在线索/商机上是主表上的 JSON 数组列, 在价格上是子表, 而「departmentId」在每个表单上
 * 都要 join 不同的表), 一套渲染必然有一部分表单算错, 而且是静默算错。</p>
 *
 * <p>聚合语句现在按表单下放到各资源 Mapper, 复用各表单列表页已有的 condition / fieldConditionJoin /
 * combine 片段; 公用的部分在 {@code CommonMapper} 里。分发入口是
 * {@code StatisticSqlMapperRegistry}, 接口是 {@code cn.cordys.common.statistic.StatisticSqlMapper}。</p>
 *
 * <p>表名全部来自 {@code FieldSourceType#getTableName()}:
 * 数据表 {@code <table>}、自定义字段表 {@code <table>_field}、大字段表 {@code <table>_field_blob}。</p>
 *
 * @author song-cc-rock
 */
public interface ExtStatisticMapper {

    /**
     * 查出该组织下指定表单上的所有统计字段, 连同字段属性。
     *
     * <p>统计字段只在宿主表单侧记录了「我统计谁」, 目标表单侧没有反向索引, 所以「谁统计了我」
     * 只能反过来找。这里直接从字段表里捞统计字段, 而不是逐个读表单配置:
     * 配置缓存里存的是整张表单的全部字段, 十几个表单读一遍再反序列化, 开销远大于按类型捞几条字段。</p>
     *
     * <p>字段表本身没有组织列, 组织隔离由 {@code sys_module_form.organization_id} 带进来;
     * {@code hostFormKeys} 用来把范围收在能承载统计字段的表单上 ——
     * 自定义表单的字段数量不受控, 不加这个条件会把它们的字段全部读出来再在 Java 里丢掉。</p>
     *
     * @param orgId        组织ID
     * @param fieldType    字段类型, 取 {@code FieldType#STATISTIC}
     * @param hostFormKeys 允许承载统计字段的表单Key集合
     *
     * @return 统计字段来源列表, 没有匹配时返回空集合
     */
    List<StatisticFieldSourceDTO> selectStatisticFields(@Param("orgId") String orgId,
                                                        @Param("fieldType") String fieldType,
                                                        @Param("hostFormKeys") Collection<String> hostFormKeys);

    /**
     * 查询目标表单某条数据在「主表列」上的值, 用于业务字段。
     *
     * <p>与 {@link #selectFieldValue} 的区别: 那个读的是自定义字段的字段值表, 这个读的是数据表自己的列。
     * 业务字段(名称、负责人、金额这类标准字段)的值都落在主表列上, 字段值表里没有它们的行。</p>
     *
     * @param dataTable 目标表单数据表名
     * @param column    主表列名, 取 {@code BusinessModuleField#getBusinessKey()}
     * @param id        数据ID
     *
     * @return 字段值, 不存在时返回 null
     */
    String selectBusinessFieldValue(@Param("dataTable") String dataTable,
                                    @Param("column") String column,
                                    @Param("id") String id);

    /**
     * 从一批数据ID里挑出还存在的那些, 用于删除后重算统计字段前把已经不存在的宿主记录剔掉。
     *
     * <p>为什么非得查一次: 删除会级联, 宿主很可能正躺在同一次删除里。以
     * {@code CustomerService#deleteCustomerResource} 为例 —— 先删客户主行与 {@code customer_field},
     * 再级联删跟进记录, 而跟进记录的宿主有可能正是这个客户(关联字段指回自己)。
     * 重算发生在这时候就会给一条已经不存在的记录写值行, 而清 {@code customer_field} 的那步已经跑过去了,
     * 孤儿行留得下来。</p>
     *
     * <p>一次查一个宿主表单(调用方按表单分组后传进来), 主键 {@code in}, 不为此再拆成逐条判断。</p>
     *
     * @param dataTable 宿主表单数据表名
     * @param ids       待判断的数据ID集合
     *
     * @return 其中仍然存在的ID, 一个都没有时返回空集合
     */
    List<String> selectExistingDataIds(@Param("dataTable") String dataTable,
                                       @Param("ids") Collection<String> ids);

    /**
     * 查询宿主表单某条数据某个字段的值, 用于解析字段对字段比较条件里的「右值」。
     *
     * @param fieldTable   字段表名
     * @param blobTable    大字段表名
     * @param fieldId      字段ID
     * @param resourceId   数据ID
     * @param blob         是否大字段
     *
     * @return 字段值, 不存在时返回 null
     */
    String selectFieldValue(@Param("fieldTable") String fieldTable,
                            @Param("blobTable") String blobTable,
                            @Param("fieldId") String fieldId,
                            @Param("resourceId") String resourceId,
                            @Param("blob") boolean blob);

    /**
     * 删除某条数据某个统计字段的旧值, 写回前先清, 保证幂等 (字段值表没有唯一索引)。
     *
     * @param fieldTable 字段表名
     * @param fieldId    统计字段ID
     * @param resourceId 数据ID
     */
    void deleteFieldValue(@Param("fieldTable") String fieldTable,
                          @Param("fieldId") String fieldId,
                          @Param("resourceId") String resourceId);

    /**
     * 写入统计值。
     *
     * @param fieldTable 字段表名
     * @param id         字段值行ID
     * @param resourceId 数据ID
     * @param fieldId    统计字段ID
     * @param fieldValue 统计值(字符串形式)
     */
    void insertFieldValue(@Param("fieldTable") String fieldTable,
                          @Param("id") String id,
                          @Param("resourceId") String resourceId,
                          @Param("fieldId") String fieldId,
                          @Param("fieldValue") String fieldValue);

    /**
     * 清除整个表单上某个已删除统计字段的历史值。
     *
     * @param fieldTable 字段表名
     * @param fieldId    统计字段ID
     *
     * @return 清理条数
     */
    int deleteFieldValuesByFieldId(@Param("fieldTable") String fieldTable,
                                   @Param("fieldId") String fieldId);
}
