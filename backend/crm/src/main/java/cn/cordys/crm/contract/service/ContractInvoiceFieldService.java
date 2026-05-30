package cn.cordys.crm.contract.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.service.BaseResourceFieldService;
import cn.cordys.crm.contract.domain.ContractInvoiceField;
import cn.cordys.crm.contract.domain.ContractInvoiceFieldBlob;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = Exception.class)
public class ContractInvoiceFieldService extends BaseResourceFieldService<ContractInvoiceField, ContractInvoiceFieldBlob> {

    @Resource
    private BaseMapper<ContractInvoiceField> contractInvoiceFieldMapper;
    @Resource
    private BaseMapper<ContractInvoiceFieldBlob> contractInvoiceFieldBlobMapper;

    @Override
    protected String getFormKey() {
        return FormKey.INVOICE.getKey();
    }

    @Override
    protected BaseMapper<ContractInvoiceField> getResourceFieldMapper() {
        return contractInvoiceFieldMapper;
    }

    @Override
    protected BaseMapper<ContractInvoiceFieldBlob> getResourceFieldBlobMapper() {
        return contractInvoiceFieldBlobMapper;
    }
}
