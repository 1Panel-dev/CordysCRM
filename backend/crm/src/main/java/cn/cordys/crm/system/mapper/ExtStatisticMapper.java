package cn.cordys.crm.system.mapper;

import cn.cordys.common.dto.condition.FilterDBCondition;
import cn.cordys.crm.system.dto.StatisticFieldSourceDTO;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

/**
 * 统计字段刷新用的通用 SQL。
 *
 * <p>统计字段的本质是「跨表单聚合」: 宿主表单的每条数据, 都要去目标表单里按关联字段捞一批数据做
 * SUM/COUNT/AVG。这套动作对每个模块都是一样的, 区别只有物理表名, 所以这里用动态表名实现,
 * 不再按模块复制十几份 SQL。</p>
 *
 * <p>表名全部来自 {@code FieldSourceType#getTableName()}:
 * 数据表 {@code <table>}、自定义字段表 {@code <table>_field}、大字段表 {@code <table>_field_blob}。</p>
 *
 * <p>筛选条件复用 {@code CommonMapper} 里已有的 {@code condition / moduleFieldCondition /
 * refFieldConditionJoin / searchMode} 片段, 保证统计口径与列表页的高级搜索完全一致。</p>
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
     * 游标分页查询宿主表单的数据ID (按主键升序)。
     *
     * <p>用游标而不是 offset: 表单数据量大时 offset 分页在大偏移下会越翻越慢,
     * 游标分页每页都是主键索引上的一次范围扫描。</p>
     *
     * @param dataTable      宿主表单数据表名
     * @param fieldTable     宿主表单自定义字段表名
     * @param fieldBlobTable 宿主表单大字段表名
     * @param orgId          组织ID
     * @param lastId         上一页最后一条ID, 首页传空
     * @param limit          每页条数
     * @param conditions     更新范围的筛选条件(高级搜索结构), 为空表示不筛选
     * @param searchMode     筛选条件之间的连接方式: AND / OR
     *
     * @return 数据ID集合
     */
    List<String> selectDataIdsByCursor(@Param("dataTable") String dataTable,
                                       @Param("fieldTable") String fieldTable,
                                       @Param("fieldBlobTable") String fieldBlobTable,
                                       @Param("orgId") String orgId,
                                       @Param("lastId") String lastId,
                                       @Param("limit") int limit,
                                       @Param("conditions") List<FilterDBCondition> conditions,
                                       @Param("searchMode") String searchMode);

    /**
     * 聚合目标表单中关联到指定记录的数据。
     *
     * <p>关联字段和被统计字段各有两种存法, 二选一, 都由调用方判定后传参:</p>
     * <ul>
     *   <li>业务字段(定义在主表上的标准字段): 值在目标表单主表的列上, 字段值表里没有它的行。
     *       关联值传 {@code relatedBusinessKey}, 被统计值传 {@code statisticBusinessKey};</li>
     *   <li>自定义字段: 值在字段值表里, 关联值传 {@code relatedFieldId}, 被统计值传 {@code statisticFieldId}。</li>
     * </ul>
     *
     * <p>两者都是业务字段时整条语句不会碰字段值表 —— 这是统计字段最常见的配置
     * (例如「合同的客户」关联 + 「合同总金额」求和), 也是它最便宜的执行路径。</p>
     *
     * @param targetTable          目标表单数据表名, 关联字段、被统计字段与组织隔离都在这张表上
     * @param fieldTable           目标表单自定义字段表名, 关联字段或被统计字段为自定义字段时才用到
     * @param fieldBlobTable       目标表单大字段表名
     * @param orgId                组织ID
     * @param relatedFieldId       目标表单中指向宿主表单的数据源字段ID, 关联字段是业务字段时传空
     * @param relatedBusinessKey   关联字段在主表上的列名, 自定义关联字段时传空
     * @param statisticFieldId     被统计的自定义字段ID(数值/计算/统计字段), COUNT 或业务字段时传空
     * @param statisticBusinessKey 被统计的业务字段在主表上的列名, 自定义字段时传空
     * @param dataId               宿主表单的数据ID, 即关联字段要等于的值
     * @param statisticType        SUM / COUNT / AVG
     * @param avgSkipEmpty         AVG 时空值是否跳过: true 不计入分母, false 当 0 计入
     * @param conditions           统计范围筛选条件(数据源字段的字段对字段比较结构), 为空表示不筛选
     * @param searchMode           筛选条件之间的连接方式: AND / OR
     *
     * @return 聚合结果, 没有命中数据时 SUM/AVG 返回 null, COUNT 返回 0
     */
    BigDecimal selectAggregate(@Param("targetTable") String targetTable,
                               @Param("fieldTable") String fieldTable,
                               @Param("fieldBlobTable") String fieldBlobTable,
                               @Param("orgId") String orgId,
                               @Param("relatedFieldId") String relatedFieldId,
                               @Param("relatedBusinessKey") String relatedBusinessKey,
                               @Param("statisticFieldId") String statisticFieldId,
                               @Param("statisticBusinessKey") String statisticBusinessKey,
                               @Param("dataId") String dataId,
                               @Param("statisticType") String statisticType,
                               @Param("avgSkipEmpty") boolean avgSkipEmpty,
                               @Param("conditions") List<FilterDBCondition> conditions,
                               @Param("searchMode") String searchMode);

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
