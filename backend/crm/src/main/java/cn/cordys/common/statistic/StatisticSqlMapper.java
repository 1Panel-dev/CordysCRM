package cn.cordys.common.statistic;

import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 统计字段刷新用的两条语句。
 *
 * <p>实现方是各个可作为统计目标的表单的资源 Mapper(线索 / 客户 / 联系人 / 跟进记录 / 跟进计划 /
 * 商机 / 产品 / 价格 / 报价单 / 合同 / 发票 / 回款计划 / 回款记录 / 订单), 各自在自己的 XML 里实现,
 * 复用各自的 {@code condition} / {@code fieldConditionJoin} / {@code combine} 片段 ——
 * 同一个条件名在不同表单上落到哪个列, 只有该表单的 Mapper 知道, 集中到一处必然猜错。</p>
 *
 * <p>方法名与参数名都是约定: 语句必须定义在具体 Mapper 自己的 XML 命名空间下
 * (MyBatis 先按具体接口的全限定名找 statement, 找不到才回溯父接口), 漏定义会在启动期直接抛
 * {@code Invalid bound statement}, 不会拖到运行时。</p>
 *
 * <p>本接口刻意不放在 {@code cn.cordys.**.mapper} 包下: 那个包被 {@code @MapperScan} 扫描,
 * 没有对应 XML 的接口会被当成 Mapper 注册而启动失败。</p>
 */
public interface StatisticSqlMapper {

    /**
     * 聚合目标表单中关联到指定宿主记录的数据。
     *
     * <p>参数必须用 {@code @Param("request")} 绑定: {@code CommonMapper} 里的统计片段直接引用
     * {@code request.xxx} 属性。</p>
     *
     * @param request 聚合请求
     *
     * @return 聚合结果; 没有数据时 NULL 或 0, 由调用方按空值口径处理
     */
    BigDecimal selectStatisticAggregate(@Param("request") StatisticAggregateRequest request);

    /**
     * 游标分页查询宿主表单的数据ID(按主键升序)。
     *
     * @param request 游标请求
     *
     * @return 本页数据ID; 没有更多数据时返回空集合
     */
    List<String> selectStatisticHostDataIds(@Param("request") StatisticCursorRequest request);
}
