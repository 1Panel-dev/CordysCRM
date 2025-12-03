package cn.cordys.crm.opportunity.service;

import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.dto.JsonDifferenceDTO;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.product.domain.Product;
import cn.cordys.crm.product.domain.ProductPrice;
import cn.cordys.crm.system.service.BaseModuleLogService;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
public class OpportunityQuotationalLogService extends BaseModuleLogService {

    @Resource
    private BaseMapper<Product> productMapper;
    @Resource
    private BaseMapper<ProductPrice> productPriceMapper;

    @Override
    public List<JsonDifferenceDTO> handleLogField(List<JsonDifferenceDTO> differenceDTOS, String orgId) {
        differenceDTOS = super.handleModuleLogField(differenceDTOS, orgId, FormKey.QUOTATION.getKey());

        for (JsonDifferenceDTO differ : differenceDTOS) {

            if (Strings.CS.equals(differ.getColumn(), BusinessModuleField.QUOTATION_PRODUCT.getBusinessKey())) {
                setProductName(differ);
                continue;
            }

            if (Strings.CS.equals(differ.getColumn(), BusinessModuleField.QUOTATION_OPPORTUNITY.getBusinessKey())) {
                setOpportunityName(differ);
                continue;
            }


            if (Strings.CS.equals(differ.getColumn(), BusinessModuleField.QUOTATION_PRODUCT_AMOUNT.getBusinessKey())) {
                differ.setColumnName(Translator.get("log.amount"));
            }

            if (differ.getColumn().contains(Translator.get("products_info"))) {
                differ.setColumnName(differ.getColumn());

            }

            if (Strings.CS.equals(differ.getColumn(), "approvalStatus")) {
                differ.setColumnName(Translator.get("log.approvalStatus"));
                if (differ.getOldValue() != null) {
                    differ.setOldValueName(Translator.get("log.approvalStatus." + differ.getOldValueName().toString()));
                }
                if (differ.getNewValue() != null) {
                    differ.setNewValueName(Translator.get("log.approvalStatus." + differ.getNewValueName().toString()));
                }
            }

        }
        return differenceDTOS;
    }


}
