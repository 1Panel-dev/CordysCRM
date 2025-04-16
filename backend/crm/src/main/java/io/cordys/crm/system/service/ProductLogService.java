package io.cordys.crm.system.service;

import io.cordys.common.constants.BusinessModuleField;
import io.cordys.common.constants.FormKey;
import io.cordys.common.dto.JsonDifferenceDTO;
import io.cordys.common.util.Translator;
import io.cordys.crm.customer.domain.CustomerContact;
import io.cordys.crm.system.domain.Product;
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
public class ProductLogService extends BaseModuleLogService {

    @Override
    public void handleLogField(List<JsonDifferenceDTO> differenceDTOS, String orgId) {
        super.handleModuleLogField(differenceDTOS, orgId, FormKey.PRODUCT.getKey());

        for (JsonDifferenceDTO differ : differenceDTOS) {
            if (StringUtils.equals(differ.getColumn(), BusinessModuleField.PRODUCT_STATUS.getBusinessKey())) {
                setProductFieldName(differ);
                break;
            }
        }
    }

    /**
     * 产品
     *
     * @param differ businessModuleField
     */
    private void setProductFieldName(JsonDifferenceDTO differ) {
        if (StringUtils.equalsIgnoreCase(differ.getOldValue().toString(), "1")){
            differ.setOldValueName(Translator.get("product.shelves"));
        }else{
            differ.setOldValueName(Translator.get("product.unShelves"));
        }
        if (StringUtils.equalsIgnoreCase(differ.getNewValue().toString(), "1")){
            differ.setOldValueName(Translator.get("product.shelves"));
        }else{
            differ.setOldValueName(Translator.get("product.unShelves"));
        }
    }
}
