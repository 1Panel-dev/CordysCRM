package cn.cordys.crm.follow.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.permission.ResourceAccessContext;
import cn.cordys.common.permission.ResourceAccessContextProvider;
import cn.cordys.crm.follow.domain.FollowUpRecord;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class FollowUpRecordResourceAccessContextProvider implements ResourceAccessContextProvider {

    @Resource
    private BaseMapper<FollowUpRecord> followUpRecordMapper;

    @Override
    public String getFormType() {
        return FormKey.FOLLOW_RECORD.getKey();
    }

    @Override
    public ResourceAccessContext getAccessContext(String resourceId, String orgId) {
        FollowUpRecord record = followUpRecordMapper.selectByPrimaryKey(resourceId);
        if (record == null) {
            return null;
        }
        ResourceAccessContext context = new ResourceAccessContext();
        context.setOwnerId(record.getOwner());
        return context;
    }

    @Override
    public Map<String, String> batchGetOwnerIds(List<String> resourceIds, String orgId) {
        if (resourceIds == null || resourceIds.isEmpty()) {
            return Map.of();
        }
        return followUpRecordMapper.selectByIds(resourceIds).stream()
                .filter(record -> record.getOwner() != null)
                .collect(Collectors.toMap(FollowUpRecord::getId, FollowUpRecord::getOwner, (a, b) -> a));
    }
}
