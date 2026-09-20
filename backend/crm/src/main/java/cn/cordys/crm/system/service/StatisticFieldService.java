package cn.cordys.crm.system.service;

import cn.cordys.crm.system.dto.field.StatisticField;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 统计字段刷新服务。
 *
 * <p>统计字段的值来源于「目标表单中通过数据源单选字段关联到当前记录的 N 条数据」, 与公式字段由前端
 * 计算提交不同, 统计值只能由后端聚合后写回, 因此需要三个触发入口:</p>
 * <ol>
 *   <li>目标表单新增或变更关联数据时自动刷新被关联记录 ({@link #refreshByRelatedDataChange});</li>
 *   <li>保存表单配置时按更新范围刷新存量数据 ({@link #refreshOnConfigSave});</li>
 *   <li>用户在详情页/编辑页手动刷新单条数据 ({@link #refreshField}).</li>
 * </ol>
 *
 * <p>统计值与该记录的字段值同库同事务, 复用 {@code custom_form_data_field} 存储,
 * 读取时由 {@code StatisticResolver} 格式化展示。</p>
 */
@Slf4j
@Service
public class StatisticFieldService {

    /**
     * 手动刷新表单数据上指定的统计字段, 并写回字段值表。
     *
     * <p>入口为 {@code POST /field/statistic/refresh/{fieldId}}; 目标表单、关联字段、被统计字段
     * 等配置信息均可由字段ID反查, 因此只需字段ID。</p>
     *
     * @param fieldId 统计字段ID
     * @param userId  用户ID
     * @param orgId   组织ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void refreshField(String fieldId, String userId, String orgId) {
        // TODO 由字段ID反查统计字段配置及其宿主表单, 按统计类型聚合目标表单的关联数据并回写字段值。
    }

    /**
     * 保存表单配置时, 按统计字段的更新范围刷新存量数据。
     *
     * <p>更新范围为「现有数据不计算」时跳过; 「全部计算」刷新当前表单下所有数据;
     * 「符合以下数据范围计算」仅刷新满足 {@code updateScopeCondition} 的数据。</p>
     *
     * @param formKey         表单Key
     * @param statisticFields 本次保存的统计字段集合
     * @param orgId           组织ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void refreshOnConfigSave(String formKey, List<StatisticField> statisticFields, String orgId) {
        // TODO 按 updateScope 筛选待刷新的数据ID, 再逐条重算并写回。
    }

    /**
     * 目标表单的关联数据新增或变更后, 自动刷新被关联记录的统计字段。
     *
     * <p>调用方为各类表单数据的保存链路; 需要按数据源字段反查到被关联的当前表单记录,
     * 只刷新那些统计字段配置中引用了该数据源字段的记录。</p>
     *
     * @param targetFormKey 发生变更的数据所属表单Key
     * @param targetDataId  发生变更的数据ID
     * @param orgId         组织ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void refreshByRelatedDataChange(String targetFormKey, String targetDataId, String orgId) {
        // TODO 反查引用了 targetFormKey 的统计字段配置及其宿主记录, 逐条重算并写回。
    }
}
