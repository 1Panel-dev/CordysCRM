package cn.cordys.crm.system.service;

import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.dto.BaseTreeNode;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.mapper.CommonMapper;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.system.constants.FieldType;
import cn.cordys.crm.system.domain.ModuleField;
import cn.cordys.crm.system.domain.ModuleFieldBlob;
import cn.cordys.crm.system.dto.field.DateTimeField;
import cn.cordys.crm.system.dto.request.FieldRepeatCheckRequest;
import cn.cordys.crm.system.dto.response.FieldRepeatCheckResponse;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ModuleFieldService {

    /**
     * 表单表格映射
     */
    private static final Map<String, String> FORM_TABLE = new HashMap<>(8);

    static {
        FORM_TABLE.put(FormKey.CLUE.getKey(), "clue");
        FORM_TABLE.put(FormKey.CUSTOMER.getKey(), "customer");
        FORM_TABLE.put(FormKey.CONTACT.getKey(), "customer_contact");
        FORM_TABLE.put(FormKey.OPPORTUNITY.getKey(), "opportunity");
        FORM_TABLE.put(FormKey.PRODUCT.getKey(), "product");
        FORM_TABLE.put(FormKey.FOLLOW_RECORD.getKey(), "follow_up_record");
        FORM_TABLE.put(FormKey.FOLLOW_PLAN.getKey(), "follow_up_plan");
    }

    @Resource
    private BaseMapper<ModuleField> moduleFieldMapper;
    @Resource
    private DepartmentService departmentService;
    @Resource
    private BaseMapper<ModuleFieldBlob> moduleFieldBlobMapper;
    @Resource
    private CommonMapper commonMapper;

    /**
     * 获取不带用户的信息的部门树
     *
     * @return 部门树
     */
    public List<BaseTreeNode> getDeptTree(String orgId) {
        return departmentService.getTree(orgId);
    }

    /**
     * 修改日期时间类型的字段部分属性
     */
    public void modifyDateProp() {
        LambdaQueryWrapper<ModuleField> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ModuleField::getType, FieldType.DATE_TIME.name());
        List<ModuleField> moduleFields = moduleFieldMapper.selectListByLambda(queryWrapper);
        List<String> fieldIds = moduleFields.stream().map(ModuleField::getId).toList();
        if (CollectionUtils.isNotEmpty(fieldIds)) {
            LambdaQueryWrapper<ModuleFieldBlob> blobWrapper = new LambdaQueryWrapper<>();
            blobWrapper.in(ModuleFieldBlob::getId, fieldIds);
            List<ModuleFieldBlob> moduleFieldBlobs = moduleFieldBlobMapper.selectListByLambda(blobWrapper);
            for (ModuleFieldBlob blob : moduleFieldBlobs) {
                DateTimeField dateTimeField = JSON.parseObject(blob.getProp(), DateTimeField.class);
                if (StringUtils.isEmpty(dateTimeField.getDateDefaultType())) {
                    dateTimeField.setDateDefaultType("custom");
                }
                blob.setProp(JSON.toJSONString(dateTimeField));
                moduleFieldBlobMapper.updateById(blob);
            }
        }
    }

    /**
     * 校验字段值是否唯一
     *
     * @param request 请求参数
     *
     * @return 是否唯一
     */
    public FieldRepeatCheckResponse checkRepeat(FieldRepeatCheckRequest request, String currentOrg) {
        ModuleField field = moduleFieldMapper.selectByPrimaryKey(request.getId());
        if (field == null) {
            throw new GenericException(Translator.get("module.field.not_exist"));
        }
        String tableName = FORM_TABLE.get(request.getFormKey());
        if (StringUtils.isBlank(tableName)) {
            throw new GenericException(Translator.get("module.form.illegal.unique.check"));
        }
        BusinessModuleField businessField = BusinessModuleField.ofKey(field.getInternalKey());
        String repeatName;
        if (businessField != null) {
            // 业务字段
            repeatName = commonMapper.checkInternalRepeatName(tableName, businessField.getBusinessKey(), request.getValue(), currentOrg);
        } else {
            repeatName = commonMapper.checkFieldRepeatName(tableName, tableName + "_field", request.getId(), request.getValue(), currentOrg);
        }
        return FieldRepeatCheckResponse.builder().name(repeatName).repeat(StringUtils.isNotBlank(repeatName)).build();
    }
}
