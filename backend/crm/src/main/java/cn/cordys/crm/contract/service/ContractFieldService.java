package cn.cordys.crm.contract.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.service.BaseResourceFieldService;
import cn.cordys.crm.contract.domain.ContractField;
import cn.cordys.crm.contract.domain.ContractFieldBlob;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = Exception.class)
public class ContractFieldService extends BaseResourceFieldService<ContractField, ContractFieldBlob> {

    @Resource
    private BaseMapper<ContractField> contractFieldMapper;
    @Resource
    private BaseMapper<ContractFieldBlob> contractFieldBlobMapper;

    @Override
    protected String getFormKey() {
        return FormKey.CONTRACT.getKey();
    }

    @Override
    protected BaseMapper<ContractField> getResourceFieldMapper() {
        return contractFieldMapper;
    }

    @Override
    protected BaseMapper<ContractFieldBlob> getResourceFieldBlobMapper() {
        return contractFieldBlobMapper;
    }
}
