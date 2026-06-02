package cn.cordys.crm.form.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.service.BaseResourceFieldService;
import cn.cordys.crm.form.domain.CustomFormDataField;
import cn.cordys.crm.form.domain.CustomFormDataFieldBlob;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(rollbackFor = Exception.class)
public class CustomFormDataFieldService extends BaseResourceFieldService<CustomFormDataField, CustomFormDataFieldBlob> {

    @Resource
    private BaseMapper<CustomFormDataField> customFormDataFieldMapper;
    @Resource
    private BaseMapper<CustomFormDataFieldBlob> customFormDataFieldBlobMapper;

    @Override
    protected String getFormKey() {
        // todo
        return UUID.randomUUID().toString();
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
