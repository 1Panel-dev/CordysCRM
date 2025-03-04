package io.cordys.crm.opportunity.service;

import io.cordys.common.service.BaseService;
import io.cordys.crm.opportunity.domain.OpportunityField;
import io.cordys.crm.opportunity.dto.request.OpportunityPageRequest;
import io.cordys.crm.opportunity.dto.response.OpportunityListResponse;
import io.cordys.crm.opportunity.mapper.ExtOpportunityMapper;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
public class OpportunityService {

    @Resource
    private ExtOpportunityMapper extOpportunityMapper;
    @Resource
    private BaseService baseService;
    @Resource
    private BaseMapper<OpportunityField> opportunityFieldMapper;

    public List<OpportunityListResponse> list(OpportunityPageRequest request, String orgId) {
        List<OpportunityListResponse> list = extOpportunityMapper.list(request, orgId);
        return buildListData(list);
    }

    private List<OpportunityListResponse> buildListData(List<OpportunityListResponse> list) {
        List<String> opportunityIds = list.stream().map(OpportunityListResponse::getId)
                .collect(Collectors.toList());
        Map<String, List<OpportunityField>> opportunityFiledMap = getOpportunityFiledMap(opportunityIds);
        list.forEach(opportunityListResponse -> {
            // 获取自定义字段
            List<OpportunityField> opportunityFields = opportunityFiledMap.get(opportunityListResponse.getId());

            if (CollectionUtils.isNotEmpty(opportunityFields)) {
                // 将毫秒数转换为天数, 并向上取整
                int days = (int) Math.ceil(opportunityListResponse.getCreateTime() * 1.0 / 86400000);
                opportunityListResponse.setReservedDays(days);
            }
        });
        return baseService.setCreateAndUpdateUserName(list);
    }


    public Map<String, List<OpportunityField>> getOpportunityFiledMap(List<String> opportunityIds) {
        if (CollectionUtils.isEmpty(opportunityIds)) {
            return Map.of();
        }
        List<OpportunityField> opportunityFields = getOpportunityFieldsByOpportunityIds(opportunityIds);
        return opportunityFields.stream().collect(Collectors.groupingBy(OpportunityField::getOpportunityId));
    }


    private List<OpportunityField> getOpportunityFieldsByOpportunityIds(List<String> opportunityIds) {
        LambdaQueryWrapper<OpportunityField> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(OpportunityField::getOpportunityId, opportunityIds);
        return opportunityFieldMapper.selectListByLambda(wrapper);
    }
}
