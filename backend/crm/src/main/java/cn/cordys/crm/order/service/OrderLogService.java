package cn.cordys.crm.order.service;

import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.dto.JsonDifferenceDTO;
import cn.cordys.common.dto.stage.StageConfigResponse;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.contract.domain.Contract;
import cn.cordys.crm.customer.domain.Customer;
import cn.cordys.crm.order.mapper.ExtOrderStageConfigMapper;
import cn.cordys.crm.system.service.BaseModuleLogService;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
public class OrderLogService extends BaseModuleLogService {

    @Resource
    private BaseMapper<Customer> customerMapper;
    @Resource
    private BaseMapper<Contract> contractMapper;
    @Resource
    private ExtOrderStageConfigMapper extOrderStageConfigMapper;

    @Override
    public List<JsonDifferenceDTO> handleLogField(List<JsonDifferenceDTO> differences, String orgId) {
        differences = super.handleModuleLogField(differences, orgId, FormKey.ORDER.getKey());

        List<StageConfigResponse> stageConfigList = extOrderStageConfigMapper.getStageConfigList(orgId);
        Map<String, String> stageMap = stageConfigList.stream()
                .collect(Collectors.toMap(StageConfigResponse::getId, StageConfigResponse::getName));
        for (var differ : differences) {
            var column = differ.getColumn();
            if (Strings.CS.equals(column, BusinessModuleField.ORDER_OWNER.getBusinessKey())) {
                setUserFieldName(differ);
                continue;
            }
            if (Strings.CS.equals(column, BusinessModuleField.ORDER_CUSTOMER.getBusinessKey())) {
                resolveCustomerName(differ);
                continue;
            }
            if (Strings.CS.equals(column, BusinessModuleField.ORDER_CONTRACT.getBusinessKey())) {
                resolveContractName(differ);
                continue;
            }
            if (Strings.CS.equals(column, "stage")) {
                differ.setColumnName(Translator.get("log.orderStage"));
                if (differ.getOldValue() != null) {
                    differ.setOldValueName(stageMap.get(differ.getOldValue()));
                }
                if (differ.getNewValue() != null) {
                    differ.setNewValueName(stageMap.get(differ.getNewValue()));
                }
            }
            if (column != null && column.contains("-")) {
                differ.setColumnName(column);
            }
            if (Strings.CI.equals(column, "approvalStatus")) {
                setApprovalName(differ);
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

    private void resolveContractName(JsonDifferenceDTO differ) {
        if (differ.getOldValue() != null) {
            resolveContract(differ.getOldValue().toString(), differ::setOldValueName);
        }
        if (differ.getNewValue() != null) {
            resolveContract(differ.getNewValue().toString(), differ::setNewValueName);
        }
    }

    private void resolveCustomer(String id, Consumer<String> nameConsumer) {
        var customer = customerMapper.selectByPrimaryKey(id);
        if (customer != null) {
            nameConsumer.accept(customer.getName());
        }
    }

    private void resolveContract(String id, Consumer<String> nameConsumer) {
        Contract contract = contractMapper.selectByPrimaryKey(id);
        if (contract != null) {
            nameConsumer.accept(contract.getName());
        }
    }
}
