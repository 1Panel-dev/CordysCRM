package cn.cordys.crm.product.service;

import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.dto.JsonDifferenceDTO;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.system.constants.FieldType;
import cn.cordys.crm.system.domain.ModuleField;
import cn.cordys.crm.system.domain.ModuleForm;
import cn.cordys.crm.system.service.BaseModuleLogService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(rollbackFor = Exception.class)
public class ProductLogService extends BaseModuleLogService {

    private static final String PRODUCT_PICTURE = "productPic";
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    @Resource
    private BaseMapper<ModuleField> moduleFieldBaseMapper;
    @Resource
    private BaseMapper<ModuleForm> moduleFormBaseMapper;

    @Override
    public List<JsonDifferenceDTO> handleLogField(List<JsonDifferenceDTO> differences, String orgId) {

        super.handleModuleLogField(differences, orgId, FormKey.PRODUCT.getKey());

        ModuleContext context = buildModuleContext(orgId);
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_TIME_FORMAT);

        for (JsonDifferenceDTO differ : differences) {
            String column = differ.getColumn();

            if (Strings.CS.equals(column, BusinessModuleField.PRODUCT_STATUS.getBusinessKey())) {
                setProductStatusName(differ);
                continue;
            }

            if (Strings.CS.equals(column, context.pictureFieldId())) {
                setProductPicName(differ);
                continue;
            }

            if (Strings.CS.equals(column, context.timeFieldId())) {
                formatDateTime(differ, dateFormat);
                continue;
            }

            if (context.attachFieldIds().contains(column)) {
                setAttachName(differ);
            }
        }
        return differences;
    }

    /**
     * 构建模块字段上下文
     */
    private ModuleContext buildModuleContext(String orgId) {

        Optional<ModuleForm> formOptional = moduleFormBaseMapper.selectListByLambda(
                new LambdaQueryWrapper<ModuleForm>()
                        .eq(ModuleForm::getOrganizationId, orgId)
                        .eq(ModuleForm::getFormKey, FormKey.PRODUCT.getKey())
        ).stream().findFirst();

        if (formOptional.isEmpty()) {
            return ModuleContext.EMPTY;
        }

        String formId = formOptional.get().getId();

        List<ModuleField> fields = moduleFieldBaseMapper.selectListByLambda(
                new LambdaQueryWrapper<ModuleField>()
                        .eq(ModuleField::getFormId, formId)
        );

        String pictureFieldId = fields.stream()
                .filter(f -> Strings.CS.equals(f.getInternalKey(), PRODUCT_PICTURE))
                .map(ModuleField::getId)
                .findFirst()
                .orElse(null);

        String timeFieldId = fields.stream()
                .filter(f -> Strings.CS.equals(f.getType(), FieldType.DATE_TIME.toString()))
                .map(ModuleField::getId)
                .findFirst()
                .orElse(null);

        List<String> attachFieldIds = fields.stream()
                .filter(f -> Strings.CS.equals(f.getType(), FieldType.ATTACHMENT.toString()))
                .map(ModuleField::getId)
                .toList();

        return new ModuleContext(pictureFieldId, timeFieldId, attachFieldIds);
    }

    private void formatDateTime(JsonDifferenceDTO differ, SimpleDateFormat format) {
        Optional.ofNullable(differ.getOldValue())
                .map(Object::toString)
                .map(Long::parseLong)
                .map(format::format)
                .ifPresent(differ::setOldValueName);

        Optional.ofNullable(differ.getNewValue())
                .map(Object::toString)
                .map(Long::parseLong)
                .map(format::format)
                .ifPresent(differ::setNewValueName);
    }

    private void setAttachName(JsonDifferenceDTO differ) {
        differ.setOldValueName(null);
        differ.setNewValueName(Translator.get("common.attachment.changed"));
    }

    /**
     * 产品图片
     */
    private void setProductPicName(JsonDifferenceDTO differ) {
        differ.setOldValueName(null);
        differ.setNewValueName(Translator.get("common.picture.changed"));
    }

    /**
     * 产品上下架状态
     */
    private void setProductStatusName(JsonDifferenceDTO differ) {
        differ.setOldValueName(resolveProductStatus(differ.getOldValue()));
        differ.setNewValueName(resolveProductStatus(differ.getNewValue()));
    }

    private String resolveProductStatus(Object value) {
        return Strings.CI.equals(String.valueOf(value), "1")
                ? Translator.get("product.shelves")
                : Translator.get("product.unShelves");
    }

    /**
     * 模块字段上下文（Java 17 record 风格，若低版本可改为普通类）
     */
    private record ModuleContext(
            String pictureFieldId,
            String timeFieldId,
            List<String> attachFieldIds
    ) {
        static final ModuleContext EMPTY =
                new ModuleContext(null, null, Collections.emptyList());
    }
}
