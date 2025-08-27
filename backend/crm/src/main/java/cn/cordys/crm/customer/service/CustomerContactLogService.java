package cn.cordys.crm.customer.service;

import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.dto.JsonDifferenceDTO;
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
public class CustomerContactLogService extends BaseModuleLogService {

    @Resource
    private BaseMapper<Customer> customerMapper;

    @Override
    public void handleLogField(List<JsonDifferenceDTO> differenceDTOS, String orgId) {
        super.handleModuleLogField(differenceDTOS, orgId, FormKey.CONTACT.getKey());

        for (JsonDifferenceDTO differ : differenceDTOS) {
            if (Strings.CS.equals(differ.getColumn(), BusinessModuleField.CUSTOMER_CONTACT_OWNER.getBusinessKey())) {
                setUserFieldName(differ);
            }
            if (Strings.CS.equals(differ.getColumn(), BusinessModuleField.CUSTOMER_CONTACT_CUSTOMER.getBusinessKey())) {
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
            }
        }
    }
}