package cn.cordys.crm.integration.agent.service;

import cn.cordys.aspectj.annotation.OperationLog;
import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.aspectj.dto.LogContextInfo;
import cn.cordys.aspectj.dto.LogDTO;
import cn.cordys.common.constants.InternalUser;
import cn.cordys.common.dto.BaseTreeNode;
import cn.cordys.common.dto.NodeSortDTO;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.NodeSortUtils;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.integration.agent.domain.AgentModule;
import cn.cordys.crm.integration.agent.dto.request.AgentModuleAddRequest;
import cn.cordys.crm.integration.agent.dto.request.AgentModuleRenameRequest;
import cn.cordys.crm.integration.agent.mapper.ExtAgentCollectionMapper;
import cn.cordys.crm.integration.agent.mapper.ExtAgentMapper;
import cn.cordys.crm.integration.agent.mapper.ExtAgentModuleMapper;
import cn.cordys.crm.system.dto.request.NodeMoveRequest;
import cn.cordys.crm.system.mapper.ExtOrganizationUserMapper;
import cn.cordys.crm.system.service.DepartmentService;
import cn.cordys.crm.system.service.LogService;
import cn.cordys.crm.system.service.MoveNodeService;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
public class AgentModuleService extends MoveNodeService {

    @Resource
    private BaseMapper<AgentModule> agentModuleMapper;
    @Resource
    private ExtAgentModuleMapper extAgentModuleMapper;
    @Resource
    private LogService logService;
    @Resource
    private ExtOrganizationUserMapper extOrganizationUserMapper;
    @Resource
    private DepartmentService departmentService;
    @Resource
    private ExtAgentMapper extAgentMapper;
    @Resource
    private ExtAgentCollectionMapper extAgentCollectionMapper;
    @Resource
    private SqlSessionFactory sqlSessionFactory;

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


    /**
     * 添加文件夹
     *
     * @param request
     * @param orgId
     * @param userId
     *
     * @return
     */
    @OperationLog(module = LogModule.AGENT, type = LogType.ADD)
    public AgentModule addFileModule(AgentModuleAddRequest request, String orgId, String userId) {
        //同一层级文件名称唯一
        checkFileName(request.getName(), request.getParentId(), orgId, null);
        String id = IDGenerator.nextStr();
        AgentModule agentModule = new AgentModule();
        agentModule.setId(id);
        agentModule.setName(request.getName());
        agentModule.setParentId(request.getParentId());
        agentModule.setOrganizationId(orgId);
        agentModule.setPos(getNextPos(orgId));
        agentModule.setCreateTime(System.currentTimeMillis());
        agentModule.setUpdateTime(System.currentTimeMillis());
        agentModule.setCreateUser(userId);
        agentModule.setUpdateUser(userId);
        agentModuleMapper.insert(agentModule);

        //日志
        OperationLogContext.setContext(LogContextInfo.builder()
                .modifiedValue(agentModule)
                .resourceId(id)
                .resourceName(agentModule.getName())
                .build());
        return agentModule;
    }


    public Long getNextPos(String orgId) {
        Long pos = extAgentModuleMapper.getNextPosByOrgId(orgId);
        return (pos == null ? 0 : pos) + NodeSortUtils.DEFAULT_NODE_INTERVAL_POS;
    }

    /**
     * 文件名重复校验
     *
     * @param name
     * @param parentId
     * @param orgId
     * @param id
     */
    private void checkFileName(String name, String parentId, String orgId, String id) {
        if (extAgentModuleMapper.countByName(name, parentId, orgId, id) > 0) {
            throw new GenericException(Translator.get("dashboard_module_name_exist"));
        }
    }


    /**
     * 重命名
     *
     * @param request
     * @param userId
     */
    @OperationLog(module = LogModule.AGENT, type = LogType.UPDATE)
    public void rename(AgentModuleRenameRequest request, String userId) {
        AgentModule originalAgentModule = checkAgentModule(request.getId());
        checkFileName(request.getName(), originalAgentModule.getParentId(), originalAgentModule.getOrganizationId(), originalAgentModule.getId());

        AgentModule agentModule = BeanUtils.copyBean(new AgentModule(), request);
        agentModule.setUpdateTime(System.currentTimeMillis());
        agentModule.setName(request.getName());
        agentModule.setUpdateUser(userId);
        agentModuleMapper.update(agentModule);

        // 添加日志上下文
        String resourceName = Optional.ofNullable(agentModule.getName()).orElse(originalAgentModule.getName());
        OperationLogContext.setContext(
                LogContextInfo.builder()
                        .originalValue(originalAgentModule)
                        .modifiedValue(checkAgentModule(request.getId()))
                        .resourceId(request.getId())
                        .resourceName(resourceName)
                        .build()
        );
    }


    /**
     * 删除文件夹
     *
     * @param ids
     * @param userId
     * @param orgId
     */
    public void delete(List<String> ids, String userId, String orgId) {
        deleteCheck(ids, orgId);
        List<AgentModule> agentModules = agentModuleMapper.selectByIds(ids);
        //刪除文件夹
        agentModuleMapper.deleteByIds(ids);
        List<LogDTO> logs = new ArrayList<>();
        // 添加日志上下文
        agentModules.forEach(agentModule -> {
            LogDTO logDTO = new LogDTO(agentModule.getOrganizationId(), agentModule.getId(), userId, LogType.DELETE, LogModule.AGENT, agentModule.getName());
            logDTO.setOriginalValue(agentModule);
            logs.add(logDTO);
        });
        logService.batchAdd(logs);
    }


    /**
     * 删除校验
     *
     * @param ids
     * @param orgId
     */
    private void deleteCheck(List<String> ids, String orgId) {
        if (extAgentModuleMapper.countAgentByIds(ids, orgId) > 0) {
            throw new GenericException(Translator.get("agent_module_cannot_delete"));
        }
    }


    /**
     * 智能体树结构
     *
     * @param orgId
     *
     * @return
     */
    public List<BaseTreeNode> getTree(String orgId) {
        List<BaseTreeNode> moduleNode = extAgentModuleMapper.selectTreeNode(orgId);
        return BaseTreeNode.buildTree(moduleNode);
    }


    private List<String> getDepartmentIds(String userId, String orgId) {
        List<String> departmentIds = new ArrayList<>();
        if (!Strings.CI.equals(userId, InternalUser.ADMIN.getValue())) {
            String departmentId = extOrganizationUserMapper.getDepartmentByUserId(userId);
            List<BaseTreeNode> departmentTree = departmentService.getTree(orgId);
            departmentIds = getParentIds(departmentTree, departmentId);
        }
        return departmentIds;
    }


    /**
     * 文件夹数量
     *
     * @param userId
     * @param orgId
     *
     * @return
     */
    public Map<String, Long> moduleCount(String userId, String orgId) {
        List<BaseTreeNode> moduleNode = extAgentModuleMapper.selectTreeNode(orgId);
        //查询模块下仪表板数据
        List<String> departmentIds = getDepartmentIds(userId, orgId);
        List<BaseTreeNode> agentNode = extAgentMapper.selectAgentNode(departmentIds, orgId, userId);

        List<BaseTreeNode> agentTreeNodes = BaseTreeNode.buildTree(moduleNode);

        Map<String, Integer> moduleCountMap = agentNode.stream()
                .collect(Collectors.groupingBy(
                        node -> node.getParentId(),
                        Collectors.summingInt(node -> 1)
                ));

        Map<String, Long> countMap = new HashMap<>();
        for (BaseTreeNode root : agentTreeNodes) {
            // 递归计算每个节点的仪表板数量
            calculateAgentCount(root, countMap, moduleCountMap);
        }

        //我的收藏
        int myCollect = extAgentCollectionMapper.countMyCollect(userId);
        countMap.put("myCollect", (long) myCollect);
        return countMap;
    }


    private int calculateAgentCount(BaseTreeNode node, Map<String, Long> countMap, Map<String, Integer> moduleCountMap) {
        int count = 0;

        if (moduleCountMap.containsKey(node.getId())) {
            count += moduleCountMap.get(node.getId());
        }

        if (node.getChildren() != null && !node.getChildren().isEmpty()) {
            for (BaseTreeNode child : node.getChildren()) {
                count += calculateAgentCount(child, countMap, moduleCountMap);
            }
        }

        countMap.put(node.getId(), (long) count);

        return count;
    }


    /**
     * 移动文件夹
     *
     * @param request
     * @param userId
     */
    public void moveNode(NodeMoveRequest request, String userId) {
        NodeSortDTO nodeSortDTO = super.getNodeSortDTO(request,
                extAgentModuleMapper::selectBaseModuleById,
                extAgentModuleMapper::selectModuleByParentIdAndPosOperator,
                false);


        AgentModule agentModule = new AgentModule();
        agentModule.setParentId(nodeSortDTO.getParent().getId());
        agentModule.setId(request.getDragNodeId());
        if (agentModuleMapper.countByExample(agentModule) == 0) {
            AgentModule moveModule = agentModuleMapper.selectByPrimaryKey(request.getDragNodeId());
            moveModule.setParentId(nodeSortDTO.getParent().getId());
            checkFileName(moveModule.getName(), moveModule.getParentId(), moveModule.getOrganizationId(), null);

            moveModule.setUpdateUser(userId);
            moveModule.setUpdateTime(System.currentTimeMillis());
            agentModuleMapper.update(moveModule);
        }

        super.sort(nodeSortDTO);
    }

    @Override
    public void updatePos(String id, long pos) {
        AgentModule agentModule = new AgentModule();
        agentModule.setId(id);
        agentModule.setPos(pos);
        agentModuleMapper.update(agentModule);
    }

    @Override
    public void refreshPos(String parentId) {
        List<String> childrenIds = extAgentModuleMapper.selectChildrenIds(parentId);
        SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH);
        ExtAgentModuleMapper batchUpdateMapper = sqlSession.getMapper(ExtAgentModuleMapper.class);
        for (int i = 0; i < childrenIds.size(); i++) {
            batchUpdateMapper.updatePos(childrenIds.get(i), i * NodeSortUtils.DEFAULT_NODE_INTERVAL_POS);
        }
        sqlSession.flushStatements();
        SqlSessionUtils.closeSqlSession(sqlSession, sqlSessionFactory);

    }
}
