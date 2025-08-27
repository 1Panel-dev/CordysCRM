package cn.cordys.crm.customer.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.service.BaseResourceFieldService;
import cn.cordys.crm.customer.domain.CustomerContactField;
import cn.cordys.crm.customer.domain.CustomerContactFieldBlob;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author jianxing
 * @date 2025-02-08 16:24:22
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class CustomerContactFieldService extends BaseResourceFieldService<CustomerContactField, CustomerContactFieldBlob> {
    @Resource
    private BaseMapper<CustomerContactField> customerContactFieldMapper;
    @Resource
    private BaseMapper<CustomerContactFieldBlob> customerContactFieldBlobMapper;

    @Override
    protected String getFormKey() {
        return FormKey.CONTACT.getKey();
    }

    @Override
    protected BaseMapper<CustomerContactField> getResourceFieldMapper() {
        return customerContactFieldMapper;
    }

    @Override
    protected BaseMapper<CustomerContactFieldBlob> getResourceFieldBlobMapper() {
        return customerContactFieldBlobMapper;
    }
}