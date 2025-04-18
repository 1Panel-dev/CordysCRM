package io.cordys.crm.system.service;

import io.cordys.common.constants.BusinessModuleField;
import io.cordys.common.constants.FormKey;
import io.cordys.common.dto.JsonDifferenceDTO;
import io.cordys.common.util.Translator;
import io.cordys.crm.system.constants.FieldType;
import io.cordys.crm.system.domain.ModuleField;
import io.cordys.crm.system.domain.ModuleForm;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
public class ProductLogService extends BaseModuleLogService {

   @Resource
   private BaseMapper<ModuleField>moduleFieldBaseMapper;
    @Resource
    private BaseMapper<ModuleForm>moduleFormBaseMapper;

    private static final String PRODUCT_PICTURE = "productPic";

    @Override
    public void handleLogField(List<JsonDifferenceDTO> differenceDTOS, String orgId) {
        super.handleModuleLogField(differenceDTOS, orgId, FormKey.PRODUCT.getKey());
        String pictureModuleFieldId= "";
        String timeModuleFieldId = "";
        List<ModuleForm> moduleForms = moduleFormBaseMapper.selectListByLambda(new LambdaQueryWrapper<ModuleForm>()
                .eq(ModuleForm::getOrganizationId, orgId)
                .eq(ModuleForm::getFormKey, FormKey.PRODUCT.getKey()));
        if (CollectionUtils.isNotEmpty(moduleForms)) {
            List<ModuleField> moduleFields = moduleFieldBaseMapper.selectListByLambda(
                    new LambdaQueryWrapper<ModuleField>()
                            .eq(ModuleField::getFormId, moduleForms.getFirst().getId())
                            .eq(ModuleField::getInternalKey, PRODUCT_PICTURE));
            if (CollectionUtils.isNotEmpty(moduleFields)) {
                pictureModuleFieldId = moduleFields.getFirst().getId();
            }
            List<ModuleField> timeModuleFields = moduleFieldBaseMapper.selectListByLambda(
                    new LambdaQueryWrapper<ModuleField>()
                            .eq(ModuleField::getFormId, moduleForms.getFirst().getId())
                            .eq(ModuleField::getType, FieldType.DATE_TIME.toString()));
            if (CollectionUtils.isNotEmpty(timeModuleFields)) {
                timeModuleFieldId = timeModuleFields.getFirst().getId();
            }
        }


        for (JsonDifferenceDTO differ : differenceDTOS) {
            if (StringUtils.equals(differ.getColumn(), BusinessModuleField.PRODUCT_STATUS.getBusinessKey())) {
                setProductFieldName(differ);
            }
            if (StringUtils.equals(differ.getColumn(), pictureModuleFieldId)) {
                setProductPicName(differ);
            }
            if (StringUtils.equals(differ.getColumn(), timeModuleFieldId)) {
                if (differ.getOldValue()!=null) {
                    differ.setOldValueName(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(differ.getOldValue()));
                }
                if (differ.getNewValue()!=null) {
                    differ.setNewValueName(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(differ.getNewValue()));
                }
            }

        }
    }

    /**
     * 产品图片
     *
     * @param differ businessModuleField
     */
    private void setProductPicName(JsonDifferenceDTO differ) {
        differ.setOldValueName("");
        differ.setNewValueName(Translator.get("common.picture.changed"));
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
            differ.setNewValueName(Translator.get("product.shelves"));
        }else{
            differ.setNewValueName(Translator.get("product.unShelves"));
        }
    }
}
