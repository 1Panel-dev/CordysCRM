package cn.cordys.crm.follow.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.permission.ResourceAccessContext;
import cn.cordys.common.permission.ResourceAccessContextProvider;
import cn.cordys.crm.follow.domain.FollowUpPlan;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class FollowUpPlanResourceAccessContextProvider implements ResourceAccessContextProvider {

    @Resource
    private BaseMapper<FollowUpPlan> followUpPlanMapper;

    @Override
    public String getFormType() {
        return FormKey.FOLLOW_PLAN.getKey();
    }

    @Override
    public ResourceAccessContext getAccessContext(String resourceId, String orgId) {
        FollowUpPlan plan = followUpPlanMapper.selectByPrimaryKey(resourceId);
        if (plan == null) {
            return null;
        }
        ResourceAccessContext context = new ResourceAccessContext();
        context.setOwnerId(plan.getOwner());
        return context;
    }

    @Override
    public Map<String, String> batchGetOwnerIds(List<String> resourceIds, String orgId) {
        if (resourceIds == null || resourceIds.isEmpty()) {
            return Map.of();
        }
        return followUpPlanMapper.selectByIds(resourceIds).stream()
                .filter(plan -> plan.getOwner() != null)
                .collect(Collectors.toMap(FollowUpPlan::getId, FollowUpPlan::getOwner, (a, b) -> a));
    }
}
