package cn.cordys.crm.clue.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.permission.ResourceAccessContext;
import cn.cordys.common.permission.ResourceAccessContextProvider;
import cn.cordys.crm.clue.domain.Clue;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ClueResourceAccessContextProvider implements ResourceAccessContextProvider {

    @Resource
    private BaseMapper<Clue> clueMapper;

    @Override
    public String getFormType() {
        return FormKey.CLUE.getKey();
    }

    @Override
    public ResourceAccessContext getAccessContext(String resourceId, String orgId) {
        Clue clue = clueMapper.selectByPrimaryKey(resourceId);
        if (clue == null) {
            return null;
        }
        ResourceAccessContext context = new ResourceAccessContext();
        context.setOwnerId(clue.getOwner());
        return context;
    }

    @Override
    public Map<String, String> batchGetOwnerIds(List<String> resourceIds, String orgId) {
        if (resourceIds == null || resourceIds.isEmpty()) {
            return Map.of();
        }
        return clueMapper.selectByIds(resourceIds).stream()
                .filter(clue -> clue.getOwner() != null)
                .collect(Collectors.toMap(Clue::getId, Clue::getOwner, (a, b) -> a));
    }
}
