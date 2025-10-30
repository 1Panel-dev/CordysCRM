package cn.cordys.crm.follow.service;

import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.dto.JsonDifferenceDTO;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.crm.system.service.BaseModuleLogService;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
public class FollowUpRecordLogService extends BaseModuleLogService {

    @Resource
    private BaseMapper<Customer> customerMapper;

    @Override
    public List<JsonDifferenceDTO> handleLogField(List<JsonDifferenceDTO> differenceDTOS, String orgId) {
        differenceDTOS = super.handleModuleLogField(differenceDTOS, orgId, FormKey.FOLLOW_RECORD.getKey());

        for (JsonDifferenceDTO differ : differenceDTOS) {
            if (Strings.CS.equals(differ.getColumn(), BusinessModuleField.FOLLOW_RECORD_CUSTOMER.getBusinessKey())) {
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

            if (Strings.CS.equals(differ.getColumn(), BusinessModuleField.FOLLOW_RECORD_CLUE.getBusinessKey())) {
                setClueName(differ);
                continue;
            }

            if (Strings.CS.equals(differ.getColumn(), BusinessModuleField.FOLLOW_RECORD_CONTENT.getBusinessKey())) {
                differ.setColumnName(Translator.get("log.follow_record_content"));
                continue;
            }

            if (Strings.CS.equals(differ.getColumn(), BusinessModuleField.FOLLOW_RECORD_OWNER.getBusinessKey())) {
                setUserFieldName(differ);
                continue;
            }

            if (Strings.CS.equals(differ.getColumn(), BusinessModuleField.FOLLOW_RECORD_CONTACT.getBusinessKey())) {
                setContactFieldName(differ);
                continue;
            }

            if (Strings.CS.equals(differ.getColumn(), BusinessModuleField.FOLLOW_RECORD_TIME.getBusinessKey())) {
                setFormatDataTimeFieldValueName(differ);
                continue;
            }

            if (Strings.CS.equals(differ.getColumn(), BusinessModuleField.FOLLOW_RECORD_OPPORTUNITY.getBusinessKey())) {
                setOpportunityName(differ);
            }
        }
        return differenceDTOS;
    }

}
