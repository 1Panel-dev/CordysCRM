package cn.cordys.crm.follow.service;

import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.dto.JsonDifferenceDTO;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.crm.follow.constants.FollowUpPlanStatusType;
import cn.cordys.crm.system.service.BaseModuleLogService;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

@Service
@Transactional(rollbackFor = Exception.class)
public class FollowUpPlanLogService extends BaseModuleLogService {

    /**
     * 跟进计划状态枚举 -> i18n 键
     */
    private static final Map<String, String> STATUS_I18N_KEYS = Map.of(
            FollowUpPlanStatusType.PREPARED.name(), "common.notStarted",
            FollowUpPlanStatusType.UNDERWAY.name(), "common.inProgress",
            FollowUpPlanStatusType.COMPLETED.name(), "common.completed",
            FollowUpPlanStatusType.CANCELLED.name(), "common.canceled");

    @Resource
    private BaseMapper<Customer> customerMapper;

    @Override
    public List<JsonDifferenceDTO> handleLogField(List<JsonDifferenceDTO> differences, String orgId) {
        differences = super.handleModuleLogField(differences, orgId, FormKey.FOLLOW_PLAN.getKey());

        for (JsonDifferenceDTO differ : differences) {
            if (Strings.CS.equals(differ.getColumn(), BusinessModuleField.FOLLOW_PLAN_CUSTOMER.getBusinessKey())) {
                if (differ.getOldValue() != null) {
                    Customer customer = customerMapper.selectByPrimaryKey(differ.getOldValue().toString());
                    if (customer != null) {
                        differ.setOldValueName(customer.getName());
                    }
                }
                if (differ.getNewValue() != null) {
                    Customer customer = customerMapper.selectByPrimaryKey(differ.getNewValue().toString());
                    if (customer != null) {
                        differ.setNewValueName(customer.getName());
                    }
                }
                continue;
            }

            if (Strings.CS.equals(differ.getColumn(), BusinessModuleField.FOLLOW_PLAN_CLUE.getBusinessKey())) {
                setClueName(differ);
                continue;
            }

            if (Strings.CS.equals(differ.getColumn(), BusinessModuleField.FOLLOW_PLAN_CONTENT.getBusinessKey())) {
                differ.setColumnName(Translator.get("log.follow_record_content"));
                continue;
            }

            if (Strings.CS.equals(differ.getColumn(), BusinessModuleField.FOLLOW_PLAN_OWNER.getBusinessKey())) {
                setUserFieldName(differ);
                continue;
            }

            if (Strings.CS.equals(differ.getColumn(), BusinessModuleField.FOLLOW_PLAN_CONTACT.getBusinessKey())) {
                setContactFieldName(differ);
                continue;
            }

            if (Strings.CS.equals(differ.getColumn(),
                    BusinessModuleField.FOLLOW_PLAN_ESTIMATED_TIME.getBusinessKey())) {
                differ.setOldValueName(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                        .format(Long.parseLong(differ.getOldValue().toString())));
                differ.setNewValueName(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                        .format(Long.parseLong(differ.getNewValue().toString())));
                continue;
            }

            if (Strings.CS.equals(differ.getColumn(), "comment")) {
                differ.setColumnName(Translator.get("log.comment"));
                continue;
            }

            if (Strings.CS.equals(differ.getColumn(), "status")) {
                differ.setColumnName(Translator.get("log.follow_plan_status"));
                differ.setOldValueName(statusName(differ.getOldValue()));
                differ.setNewValueName(statusName(differ.getNewValue()));
                continue;
            }

            if (Strings.CS.equals(differ.getColumn(), BusinessModuleField.FOLLOW_PLAN_OPPORTUNITY.getBusinessKey())) {
                setOpportunityName(differ);
            }
        }
        return differences;
    }

    /**
     * 跟进计划状态枚举值转文案, 未知值原样返回
     */
    private String statusName(Object value) {
        if (value == null) {
            return null;
        }
        String status = value.toString();
        return Translator.get(STATUS_I18N_KEYS.getOrDefault(status, status), status);
    }

}
