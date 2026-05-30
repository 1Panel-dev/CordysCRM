package cn.cordys.crm.contract.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.service.BaseResourceFieldService;
import cn.cordys.crm.contract.domain.ContractPaymentPlanField;
import cn.cordys.crm.contract.domain.ContractPaymentPlanFieldBlob;
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
public class ContractPaymentPlanFieldService extends BaseResourceFieldService<ContractPaymentPlanField, ContractPaymentPlanFieldBlob> {
    @Resource
    private BaseMapper<ContractPaymentPlanField> contractPaymentPlanFieldMapper;
    @Resource
    private BaseMapper<ContractPaymentPlanFieldBlob> contractPaymentPlanFieldBlobMapper;

    @Override
    protected String getFormKey() {
        return FormKey.CONTRACT_PAYMENT_PLAN.getKey();
    }

    @Override
    protected BaseMapper<ContractPaymentPlanField> getResourceFieldMapper() {
        return contractPaymentPlanFieldMapper;
    }

    @Override
    protected BaseMapper<ContractPaymentPlanFieldBlob> getResourceFieldBlobMapper() {
        return contractPaymentPlanFieldBlobMapper;
    }
}