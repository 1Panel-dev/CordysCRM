package cn.cordys.crm.contract.service;

import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.dto.JsonDifferenceDTO;
import cn.cordys.crm.contract.constants.ContractApprovalStatus;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.crm.system.service.BaseModuleLogService;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = Exception.class)
public class ContractLogService extends BaseModuleLogService {

  @Resource private BaseMapper<Customer> customerMapper;

  @Override
  public List<JsonDifferenceDTO> handleLogField(List<JsonDifferenceDTO> differences, String orgId) {
    differences = super.handleModuleLogField(differences, orgId, FormKey.CONTRACT.getKey());

    for (JsonDifferenceDTO differ : differences) {

      if (Strings.CS.equals(
          differ.getColumn(), BusinessModuleField.CONTRACT_OWNER.getBusinessKey())) {
        setUserFieldName(differ);
        continue;
      }

      if (Strings.CS.equals(
          differ.getColumn(), BusinessModuleField.CONTRACT_CUSTOMER_NAME.getBusinessKey())) {
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

      if (Strings.CI.equals(differ.getColumn(), "approvalStatus")
          && Arrays.stream(ContractApprovalStatus.values())
              .anyMatch(status -> status.name().equals(differ.getOldValue()))) {
        setApprovalName(differ);
      }

      if (differ.getColumn().contains("-")) {
        differ.setColumnName(differ.getColumn());
      }
    }

    return differences;
  }
}
