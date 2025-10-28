package cn.cordys.crm.system.service;

import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.dto.JsonDifferenceDTO;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.system.constants.FieldType;
import cn.cordys.crm.system.domain.ModuleField;
import cn.cordys.crm.system.domain.ModuleForm;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
public class ProductLogService extends BaseModuleLogService {

    private static final String PRODUCT_PICTURE = "productPic";
    @Resource
    private BaseMapper<ModuleField> moduleFieldBaseMapper;
    @Resource
    private BaseMapper<ModuleForm> moduleFormBaseMapper;

    @Override
    public void handleLogField(List<JsonDifferenceDTO> differenceDTOS, String orgId) {
        super.handleModuleLogField(differenceDTOS, orgId, FormKey.PRODUCT.getKey());
        String pictureModuleFieldId = "";
        String timeModuleFieldId = "";
        List<String> attachModuleFieldIds = new ArrayList<>();
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
            List<ModuleField> attachModuleFields = moduleFieldBaseMapper.selectListByLambda(
                    new LambdaQueryWrapper<ModuleField>()
                            .eq(ModuleField::getFormId, moduleForms.getFirst().getId())
                            .eq(ModuleField::getType, FieldType.ATTACHMENT.toString()));
            if (CollectionUtils.isNotEmpty(attachModuleFields)) {
                attachModuleFieldIds = attachModuleFields.stream().map(ModuleField::getId).toList();
            }
        }


        for (JsonDifferenceDTO differ : differenceDTOS) {
            if (Strings.CS.equals(differ.getColumn(), BusinessModuleField.PRODUCT_STATUS.getBusinessKey())) {
                setProductFieldName(differ);
            }
            if (Strings.CS.equals(differ.getColumn(), pictureModuleFieldId)) {
                setProductPicName(differ);
            }
            if (Strings.CS.equals(differ.getColumn(), timeModuleFieldId)) {
                if (differ.getOldValue() != null) {
                    differ.setOldValueName(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Long.parseLong(differ.getOldValue().toString())));
                }
                if (differ.getNewValue() != null) {
                    differ.setNewValueName(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Long.parseLong(differ.getNewValue().toString())));
                }
            }
            if (CollectionUtils.isNotEmpty(attachModuleFieldIds)&& attachModuleFieldIds.contains(differ.getColumn())) {
                setAttachName(differ);
            }
        }
    }

    private void setAttachName(JsonDifferenceDTO differ) {
        differ.setOldValueName("");
        differ.setNewValueName(Translator.get("common.attachment.changed"));
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
        if (Strings.CI.equals(differ.getOldValue().toString(), "1")) {
            differ.setOldValueName(Translator.get("product.shelves"));
        } else {
            differ.setOldValueName(Translator.get("product.unShelves"));
        }
        if (Strings.CI.equals(differ.getNewValue().toString(), "1")) {
            differ.setNewValueName(Translator.get("product.shelves"));
        } else {
            differ.setNewValueName(Translator.get("product.unShelves"));
        }
    }
}
