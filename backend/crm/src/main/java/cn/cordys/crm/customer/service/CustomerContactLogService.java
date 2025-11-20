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
    public List<JsonDifferenceDTO> handleLogField(List<JsonDifferenceDTO> differenceDTOS, String orgId) {
        List<JsonDifferenceDTO> result = super.handleModuleLogField(
                differenceDTOS, orgId, FormKey.CONTACT.getKey()
        );

        for (JsonDifferenceDTO differ : result) {
            String column = differ.getColumn();

            // 负责人字段处理
            if (Strings.CS.equals(column, BusinessModuleField.CUSTOMER_CONTACT_OWNER.getBusinessKey())) {
                setUserFieldName(differ);
                continue;
            }

            // 客户字段处理
            if (Strings.CS.equals(column, BusinessModuleField.CUSTOMER_CONTACT_CUSTOMER.getBusinessKey())) {
                differ.setOldValueName(getCustomerName(differ.getOldValue()));
                differ.setNewValueName(getCustomerName(differ.getNewValue()));
            }
        }

        return result;
    }

    /**
     * 根据客户ID获取客户名称
     */
    private String getCustomerName(Object customerId) {
        if (customerId == null) {
            return null;
        }

        Customer customer = customerMapper.selectByPrimaryKey(customerId.toString());
        return customer != null ? customer.getName() : null;
    }
}