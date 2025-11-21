package cn.cordys.crm.contract.service;

import cn.cordys.common.util.BeanUtils;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.service.BaseService;
import cn.cordys.crm.contract.dto.request.*;
import cn.cordys.crm.contract.dto.response.*;
import cn.cordys.crm.contract.mapper.ExtContractPaymentPlanMapper;
import cn.cordys.crm.contract.domain.ContractPaymentPlan;

/**
 *
 * @author jianxing
 * @date 2025-11-21 15:11:29
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class ContractPaymentPlanService {

    @Resource
    private BaseMapper<ContractPaymentPlan> contractPaymentPlanMapper;

    @Resource
    private ExtContractPaymentPlanMapper extContractPaymentPlanMapper;

    @Resource
    private BaseService baseService;

    public List<ContractPaymentPlanListResponse> list(ContractPaymentPlanPageRequest request, String orgId) {
        List<ContractPaymentPlanListResponse> list = extContractPaymentPlanMapper.list(request, orgId);
        // todo

        return baseService.setCreateAndUpdateUserName(list);
    }


    public ContractPaymentPlanGetResponse get(String id) {
        ContractPaymentPlan contractPaymentPlan = contractPaymentPlanMapper.selectByPrimaryKey(id);
        ContractPaymentPlanGetResponse contractPaymentPlanGetResponse = BeanUtils.copyBean(new ContractPaymentPlanGetResponse(), contractPaymentPlan);
        // todo
        return baseService.setCreateAndUpdateUserName(contractPaymentPlanGetResponse);
    }

    public ContractPaymentPlan add(ContractPaymentPlanAddRequest request, String userId, String orgId) {
        ContractPaymentPlan contractPaymentPlan = BeanUtils.copyBean(new ContractPaymentPlan(), request);
        contractPaymentPlan.setCreateTime(System.currentTimeMillis());
        contractPaymentPlan.setUpdateTime(System.currentTimeMillis());
        contractPaymentPlan.setUpdateUser(userId);
        contractPaymentPlan.setCreateUser(userId);
        contractPaymentPlan.setOrganizationId(orgId);
        contractPaymentPlan.setId(IDGenerator.nextStr());
        contractPaymentPlanMapper.insert(contractPaymentPlan);
        return contractPaymentPlan;
    }

    public ContractPaymentPlan update(ContractPaymentPlanUpdateRequest request, String userId) {
        ContractPaymentPlan contractPaymentPlan = BeanUtils.copyBean(new ContractPaymentPlan(), request);
        contractPaymentPlan.setUpdateTime(System.currentTimeMillis());
        contractPaymentPlan.setUpdateUser(userId);
        contractPaymentPlanMapper.update(contractPaymentPlan);
        return contractPaymentPlanMapper.selectByPrimaryKey(contractPaymentPlan.getId());
    }

    public void delete(String id) {
        contractPaymentPlanMapper.deleteByPrimaryKey(id);
    }
}