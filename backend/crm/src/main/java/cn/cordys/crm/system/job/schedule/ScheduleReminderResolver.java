package cn.cordys.crm.system.job.schedule;

import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.constants.FormKey;
import cn.cordys.crm.system.constants.FieldType;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.domain.MessageTask;
import cn.cordys.crm.system.domain.ModuleField;
import cn.cordys.crm.system.domain.ModuleForm;
import cn.cordys.crm.system.mapper.ExtScheduleReminderMapper;
import cn.cordys.crm.system.service.ModuleFieldService;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 定时提醒通用解析器
 * <p>
 * 由配置的 fieldId 反查字段元数据，定位业务主表并区分内置/自定义，任何系统模块与自定义表单都适用：
 * <ul>
 *   <li>fieldId 命中 {@code sys_module_field.id} → field → form → 主表；
 *       无 internalKey 视为自定义字段（join {@code <表>_field}），有 internalKey 视为内置字段（映射主表列）。</li>
 *   <li>系统表单走 {@code FormKey}→主表映射；自定义表单数据在 {@code custom_form_data}，按 {@code custom_form_id} 过滤。</li>
 *   <li>fieldId 不是模块字段 id 时，兼容旧的“fieldId 直接是内置列名”写法（按 event 兜底定位主表）。</li>
 * </ul>
 * 通知展示名称统一取业务记录自身名称（无 name 时取 content/id）。
 * </p>
 */
@Component
public class ScheduleReminderResolver {

    private static final String CUSTOM_FORM_TABLE = "custom_form_data";
    private static final String CUSTOM_FORM_FIELD_TABLE = "custom_form_data_field";
    private static final String CUSTOM_FORM_ID_COLUMN = "custom_form_id";

    private static final String ID = "id";
    private static final String NAME = "name";
    private static final String CONTENT = "content";
    private static final String OWNER = "owner";
    private static final String CREATE_USER = "create_user";
    private static final String DUE_TIME = "due_time";

    /**
     * 旧格式（fieldId 为内置列名）按 event 兜底定位主表
     */
    private static final Map<String, String> EVENT_TABLE = Map.of(
            NotificationConstants.Event.CONTRACT_EXPIRED, "contract",
            NotificationConstants.Event.CONTRACT_EXPIRING, "contract",
            NotificationConstants.Event.BUSINESS_QUOTATION_EXPIRED, "opportunity_quotation",
            NotificationConstants.Event.BUSINESS_QUOTATION_EXPIRING, "opportunity_quotation",
            // NotificationConstants.Event.CONTRACT_PAYMENT_EXPIRED, "contract_payment_plan",
            // NotificationConstants.Event.CONTRACT_PAYMENT_EXPIRING, "contract_payment_plan",
            NotificationConstants.Event.FOLLOW_UP_PLAN_DUE, "follow_up_plan");

    @Resource
    private BaseMapper<ModuleField> moduleFieldMapper;
    @Resource
    private BaseMapper<ModuleForm> moduleFormMapper;
    @Resource
    private ModuleFieldService moduleFieldService;
    @Resource
    private ExtScheduleReminderMapper extScheduleReminderMapper;

    public List<ScheduleHit> findDue(MessageTask task, String fieldId, long start, long end) {
        if (StringUtils.isBlank(fieldId)) {
            return List.of();
        }
        Target target = resolveTarget(task, fieldId);
        if (target == null) {
            return List.of();
        }
        List<Map<String, Object>> rows = target.builtInColumn() != null
                ? extScheduleReminderMapper.selectRowsByColumn(target.mainTable(), target.builtInColumn(),
                        task.getOrganizationId(), start, end, target.formColumn(), target.formValue())
                : extScheduleReminderMapper.selectRowsByCustomField(target.mainTable(), target.fieldTable(), fieldId,
                        task.getOrganizationId(), start, end, target.formColumn(), target.formValue());
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<ScheduleHit> hits = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            ScheduleHit hit = toHit(row, target);
            if (hit != null) {
                hits.add(hit);
            }
        }
        return hits;
    }

    private Target resolveTarget(MessageTask task, String fieldId) {
        ModuleField moduleField = moduleFieldMapper.selectByPrimaryKey(fieldId);
        if (moduleField != null) {
            // 只支持日期时间类型字段作为提醒锚点
            if (!FieldType.DATE_TIME.name().equals(moduleField.getType())) {
                return null;
            }
            ModuleForm form = moduleFormMapper.selectByPrimaryKey(moduleField.getFormId());
            if (form == null || StringUtils.isBlank(form.getFormKey())) {
                return null;
            }
            String formKey = form.getFormKey();
            boolean systemForm = FormKey.ofKey(formKey) != null;
            String mainTable = systemForm ? moduleFieldService.getTableByFormKey(formKey) : CUSTOM_FORM_TABLE;
            if (StringUtils.isBlank(mainTable)) {
                return null;
            }
            String formColumn = systemForm ? null : CUSTOM_FORM_ID_COLUMN;
            String formValue = systemForm ? null : formKey;
            if (StringUtils.isBlank(moduleField.getInternalKey())) {
                // 自定义日期字段：join <表>_field（自定义表单固定 custom_form_data_field）
                String fieldTable = systemForm ? mainTable + "_field" : CUSTOM_FORM_FIELD_TABLE;
                return new Target(mainTable, fieldTable, null, formColumn, formValue);
            }
            // 内置字段：internalKey → 主表列
            String column = columnOfInternalKey(moduleField.getInternalKey());
            if (column == null) {
                return null;
            }
            return new Target(mainTable, null, column, formColumn, formValue);
        }
        // 兼容旧格式：fieldId 直接是主表内置列名（按 event 兜底定位主表）
        if (!isSafeIdentifier(fieldId)) {
            return null;
        }
        String table = EVENT_TABLE.get(task.getEvent());
        if (table == null) {
            return null;
        }
        return new Target(table, null, camelToSnake(fieldId), null, null);
    }

    private String columnOfInternalKey(String internalKey) {
        for (BusinessModuleField field : BusinessModuleField.values()) {
            if (StringUtils.equals(field.getKey(), internalKey)) {
                return camelToSnake(field.getBusinessKey());
            }
        }
        return null;
    }

    private ScheduleHit toHit(Map<String, Object> row, Target target) {
        String id = str(row.get(ID));
        if (StringUtils.isBlank(id) || isInactive(row)) {
            return null;
        }
        Long due = target.builtInColumn() != null ? number(row.get(target.builtInColumn())) : number(row.get(DUE_TIME));
        if (due == null) {
            return null;
        }
        String name = str(row.get(NAME));
        if (StringUtils.isBlank(name)) {
            name = str(row.get(CONTENT));
        }
        if (StringUtils.isBlank(name)) {
            name = id;
        }
        String owner = str(row.get(OWNER));
        if (StringUtils.isBlank(owner)) {
            owner = str(row.get(CREATE_USER));
        }
        return new ScheduleHit(id, name, owner, str(row.get(CREATE_USER)), due);
    }

    /**
     * 通用失效判断：作废/已完成/已取消/已转记录的不再提醒（列存在才判断）
     */
    private boolean isInactive(Map<String, Object> row) {
        if ("VOIDED".equals(str(row.get("approval_status")))) {
            return true;
        }
        String stage = str(row.get("stage"));
        if ("VOID".equals(stage) || "COMPLETED_PERFORMANCE".equals(stage)) {
            return true;
        }
        if (isTruthy(row.get("converted"))) {
            return true;
        }
        String status = str(row.get("status"));
        return "COMPLETED".equals(status) || "CANCELLED".equals(status);
    }

    private boolean isTruthy(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.longValue() != 0;
        }
        String text = value.toString();
        return "true".equalsIgnoreCase(text) || "1".equals(text);
    }

    private Long number(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String str(Object value) {
        return value == null ? null : value.toString();
    }

    private boolean isSafeIdentifier(String value) {
        return value.matches("[A-Za-z_][A-Za-z0-9_]*");
    }

    private String camelToSnake(String value) {
        if (StringUtils.isBlank(value)) {
            return value;
        }
        return value.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
    }

    /**
     * @param mainTable     业务主表
     * @param fieldTable    自定义字段值表（内置字段时为 null）
     * @param builtInColumn 内置时间列（自定义字段时为 null）
     * @param formColumn    自定义表单过滤列（系统表单为 null）
     * @param formValue     自定义表单过滤值（系统表单为 null）
     */
    private record Target(String mainTable, String fieldTable, String builtInColumn, String formColumn,
            String formValue) {
    }
}
