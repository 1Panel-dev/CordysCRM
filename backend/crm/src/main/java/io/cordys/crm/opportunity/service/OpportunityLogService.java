package io.cordys.crm.opportunity.service;

import io.cordys.common.constants.BusinessModuleField;
import io.cordys.common.constants.FormKey;
import io.cordys.common.dto.JsonDifferenceDTO;
import io.cordys.common.util.Translator;
import io.cordys.crm.customer.domain.CustomerContact;
import io.cordys.crm.system.domain.Product;
import io.cordys.crm.system.service.BaseModuleLogService;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(rollbackFor = Exception.class)
public class OpportunityLogService extends BaseModuleLogService {

    @Resource
    private BaseMapper<Product> productMapper;
    @Resource
    private BaseMapper<CustomerContact> customerContactMapper;


    @Override
    public void handleLogField(List<JsonDifferenceDTO> differenceDTOS, String orgId) {
        super.handleModuleLogField(differenceDTOS, orgId, FormKey.OPPORTUNITY.getKey());

        for (JsonDifferenceDTO differ : differenceDTOS) {
            if (StringUtils.equals(differ.getColumn(), BusinessModuleField.OPPORTUNITY_OWNER.getBusinessKey())) {
                setUserFieldName(differ);
                continue;
            }

            if (StringUtils.equals(differ.getColumn(), BusinessModuleField.OPPORTUNITY_PRODUCTS.getBusinessKey())) {
                setProductFieldName(differ);
                continue;
            }

            if (StringUtils.equals(differ.getColumn(), BusinessModuleField.OPPORTUNITY_CONTACT.getBusinessKey())) {
                setContactFieldName(differ);
                continue;
            }

            if (StringUtils.equals(differ.getColumn(), BusinessModuleField.OPPORTUNITY_CUSTOMER_NAME.getBusinessKey())) {
                differ.setColumnName(Translator.get("log.customerId"));
                setCustomerName(differ);
                continue;
            }

            if (StringUtils.equals(differ.getColumnName(), Translator.get("log.stage"))) {
                differ.setOldValueName(Translator.get(differ.getOldValue().toString()));
                differ.setNewValueName(Translator.get(differ.getNewValue().toString()));
            }

            if (StringUtils.equals(differ.getColumn(), "status")) {
                differ.setColumnName(Translator.get("log.opportunity." + differ.getColumn().toString()));
                differ.setOldValueName(Boolean.valueOf(differ.getOldValueName().toString()) ? Translator.get("log.opportunity.status.true") : Translator.get("log.opportunity.status.false"));
                differ.setNewValueName(Boolean.valueOf(differ.getNewValueName().toString()) ? Translator.get("log.opportunity.status.true") : Translator.get("log.opportunity.status.false"));
            }
        }
    }

    /**
     * 产品
     *
     * @param differ
     */
    private void setProductFieldName(JsonDifferenceDTO differ) {
        Optional.ofNullable(differ.getOldValue()).ifPresent(oldValue -> {
            List<String> ids = ((Collection<?>) oldValue).stream()
                    .map(String::valueOf)
                    .toList();
            List<Product> products = productMapper.selectByIds(ids);
            differ.setOldValueName(products.stream().map(Product::getName).toList());
        });

        Optional.ofNullable(differ.getNewValue()).ifPresent(newValue -> {
            List<String> ids = ((Collection<?>) newValue).stream()
                    .map(String::valueOf)
                    .toList();
            List<Product> products = productMapper.selectByIds(ids);
            differ.setNewValueName(products.stream().map(Product::getName).toList());
        });
    }
}
