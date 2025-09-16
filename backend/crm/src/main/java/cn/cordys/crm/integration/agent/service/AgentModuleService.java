package cn.cordys.crm.integration.agent.service;

import cn.cordys.common.dto.BaseTreeNode;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.integration.agent.domain.AgentModule;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(rollbackFor = Exception.class)
public class AgentModuleService {

    @Resource
    private BaseMapper<AgentModule> agentModuleMapper;

    public AgentModule checkAgentModule(String id) {
        AgentModule agentModule = agentModuleMapper.selectByPrimaryKey(id);
        if (agentModule == null) {
            throw new GenericException(Translator.get("agent_module_blank"));
        }
        return agentModule;
    }

    public List<String> getParentIds(List<BaseTreeNode> departmentTree, String departmentId) {
        List<String> ids = new ArrayList<>();
        if (CollectionUtils.isEmpty(departmentTree) || StringUtils.isBlank(departmentId)) {
            return ids;
        }

        Map<String, BaseTreeNode> idNodeMap = new HashMap<>();
        buildIdNodeMap(departmentTree, idNodeMap);

        BaseTreeNode currentNode = idNodeMap.get(departmentId);
        while (currentNode != null && currentNode.getParentId() != null) {
            ids.add(currentNode.getId());
            if ("NONE".equals(currentNode.getParentId())) {
                break;
            }
            currentNode = idNodeMap.get(currentNode.getParentId());
        }

        return ids;
    }

    private static void buildIdNodeMap(List<BaseTreeNode> nodeList, Map<String, BaseTreeNode> idNodeMap) {
        for (BaseTreeNode node : nodeList) {
            idNodeMap.put(node.getId(), node);
            if (CollectionUtils.isNotEmpty(node.getChildren())) {
                buildIdNodeMap(node.getChildren(), idNodeMap);
            }
        }
    }
}
