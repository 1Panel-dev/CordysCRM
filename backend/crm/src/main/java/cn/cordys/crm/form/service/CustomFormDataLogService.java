package cn.cordys.crm.form.service;

import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.dto.JsonDifferenceDTO;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.form.domain.CustomFormData;
import cn.cordys.crm.system.service.BaseModuleLogService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class CustomFormDataLogService extends BaseModuleLogService {

    @Override
    public List<JsonDifferenceDTO> handleLogField(List<JsonDifferenceDTO> differences, String orgId) {
        String jsonValue = StringUtils.isNotBlank(oldValue) ? oldValue : newValue;
        if (StringUtils.isBlank(jsonValue)) {
            return differences;
        }

        CustomFormData customFormData;
        try {
            customFormData = JSON.parseObject(jsonValue, CustomFormData.class);
        } catch (Exception e) {
            log.warn("解析自定义表单数据日志失败, 尝试修复特殊字符后重新解析: {}", e.getMessage());
            // 对含有未转义特殊字符的JSON进行修复
            String fixedJson = escapeSpecialChars(jsonValue);
            try {
                customFormData = JSON.parseObject(fixedJson, CustomFormData.class);
            } catch (Exception ex) {
                log.error("修复后仍然无法解析自定义表单数据日志: {}", ex.getMessage());
                return differences;
            }
        }

        final String formId = customFormData.getCustomFormId();
        differences.removeIf(differ -> Strings.CS.equalsAny(differ.getColumn(), "customFormId"));
        differences = super.handleModuleLogField(differences, orgId, formId);
        for (JsonDifferenceDTO differ : differences) {
            if (Strings.CS.equals(differ.getColumn(), BusinessModuleField.CUSTOM_FORM_DATA_OWNER.getBusinessKey())) {
                setUserFieldName(differ);
            }
        }
        return differences;
    }

    /**
     * 修复JSON字符串中未转义的特殊字符
     * 将未转义的控制字符（如换行符、制表符等）转换为转义形式
     *
     * @param json 原始JSON字符串
     * @return 修复后的JSON字符串
     */
    private String escapeSpecialChars(String json) {
        if (json == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(json.length());
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            switch (c) {
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 0x20) {
                        // 其他控制字符使用Unicode转义
                        sb.append("\\u").append(String.format("%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }
}
