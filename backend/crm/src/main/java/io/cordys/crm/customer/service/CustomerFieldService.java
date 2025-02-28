package io.cordys.crm.customer.service;

import io.cordys.common.constants.FormKey;
import io.cordys.common.service.BaseResourceFieldService;
import io.cordys.crm.customer.domain.CustomerField;
import io.cordys.crm.customer.domain.CustomerFieldBlob;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author jianxing
 * @date 2025-02-08 16:24:22
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class CustomerFieldService extends BaseResourceFieldService<CustomerField, CustomerFieldBlob> {
    @Resource
    private BaseMapper<CustomerField> customerFieldMapper;
    @Resource
    private BaseMapper<CustomerFieldBlob> customerFieldBlobMapper;

    @Override
    protected String getFormKey() {
        return FormKey.CUSTOMER.getKey();
    }

    @Override
    protected BaseMapper<CustomerField> getResourceFieldMapper() {
        return customerFieldMapper;
    }

    @Override
    protected BaseMapper<CustomerFieldBlob> getResourceFieldBlobMapper() {
        return customerFieldBlobMapper;
    }
}