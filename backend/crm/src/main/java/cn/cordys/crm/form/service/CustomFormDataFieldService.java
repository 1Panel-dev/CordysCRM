package cn.cordys.crm.form.service;

import cn.cordys.common.service.BaseResourceFieldService;
import cn.cordys.crm.form.domain.CustomFormDataField;
import cn.cordys.crm.form.domain.CustomFormDataFieldBlob;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = Exception.class)
public class CustomFormDataFieldService extends BaseResourceFieldService<CustomFormDataField, CustomFormDataFieldBlob> {

    private static final ThreadLocal<String> FORM_KEY_HOLDER = new ThreadLocal<>();

    @Resource
    private BaseMapper<CustomFormDataField> customFormDataFieldMapper;
    @Resource
    private BaseMapper<CustomFormDataFieldBlob> customFormDataFieldBlobMapper;

    public static void setFormKey(String formKey) {
        FORM_KEY_HOLDER.set(formKey);
    }

    public static void clearFormKey() {
        FORM_KEY_HOLDER.remove();
    }

    @Override
    protected String getFormKey() {
        String formKey = FORM_KEY_HOLDER.get();
        if (formKey == null) {
            throw new IllegalStateException("formKey 没有设置，请先调用 setFormKey 方法设置 formKey");
        }
        return formKey;
    }

    @Override
    protected BaseMapper<CustomFormDataField> getResourceFieldMapper() {
        return customFormDataFieldMapper;
    }

    @Override
    protected BaseMapper<CustomFormDataFieldBlob> getResourceFieldBlobMapper() {
        return customFormDataFieldBlobMapper;
    }
}
