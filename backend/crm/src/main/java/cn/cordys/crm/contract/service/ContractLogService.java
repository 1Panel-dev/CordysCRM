package cn.cordys.crm.contract.service;

import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.dto.JsonDifferenceDTO;
import cn.cordys.crm.contract.constants.ContractApprovalStatus;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.crm.system.service.BaseModuleLogService;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

@Service
@Transactional(rollbackFor = Exception.class)
public class ContractLogService extends BaseModuleLogService {

    @Resource
    private BaseMapper<Customer> customerMapper;

    @Override
    public List<JsonDifferenceDTO> handleLogField(List<JsonDifferenceDTO> differences, String orgId) {
        differences = super.handleModuleLogField(differences, orgId, FormKey.CONTRACT.getKey());
        for (var differ : differences) {
            var column = differ.getColumn();
            if (Strings.CS.equals(column, BusinessModuleField.CONTRACT_OWNER.getBusinessKey())) {
                setUserFieldName(differ);
                continue;
            }
            if (Strings.CS.equals(column, BusinessModuleField.CONTRACT_CUSTOMER_NAME.getBusinessKey())) {
                resolveCustomerName(differ);
                continue;
            }
            if (Strings.CI.equals(column, "approvalStatus") && isContractApprovalStatus(differ.getOldValue())) {
                setApprovalName(differ);
            }
            if (column != null && column.contains("-")) {
                differ.setColumnName(column);
            }
        }
        return differences;
    }

    private void resolveCustomerName(JsonDifferenceDTO differ) {
        if (differ.getOldValue() != null) {
            resolveCustomer(differ.getOldValue().toString(), differ::setOldValueName);
        }
        if (differ.getNewValue() != null) {
            resolveCustomer(differ.getNewValue().toString(), differ::setNewValueName);
        }
    }

    private void resolveCustomer(String id, Consumer<String> nameConsumer) {
        var customer = customerMapper.selectByPrimaryKey(id);
        if (customer != null) {
            nameConsumer.accept(customer.getName());
        }
    }

    private boolean isContractApprovalStatus(Object value) {
        if (value == null) {
            return false;
        }
        var text = value.toString();
        return Arrays.stream(ContractApprovalStatus.values())
                     .anyMatch(status -> status.name().equals(text));
    }
}
