package cn.cordys.crm.dashboard.service;

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
import cn.cordys.crm.dashboard.domain.DashboardModule;
import cn.cordys.crm.dashboard.dto.DashboardTreeNode;
import cn.cordys.crm.dashboard.dto.request.DashboardModuleAddRequest;
import cn.cordys.crm.dashboard.dto.request.DashboardModuleRenameRequest;
import cn.cordys.crm.dashboard.mapper.ExtDashboardCollectionMapper;
import cn.cordys.crm.dashboard.mapper.ExtDashboardMapper;
import cn.cordys.crm.dashboard.mapper.ExtDashboardModuleMapper;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
public class DashboardModuleService extends MoveNodeService {

    @Resource
    private ExtDashboardModuleMapper extDashboardModuleMapper;
    @Resource
    private BaseMapper<DashboardModule> dashboardModuleMapper;
    @Resource
    private LogService logService;
    @Resource
    private ExtDashboardMapper extDashboardMapper;
    @Resource
    private ExtDashboardCollectionMapper extDashboardCollectionMapper;
    @Resource
    private ExtOrganizationUserMapper extOrganizationUserMapper;
    @Resource
    private DepartmentService departmentService;

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
    @OperationLog(module = LogModule.DASHBOARD, type = LogType.ADD)
    public DashboardModule addFileModule(DashboardModuleAddRequest request, String orgId, String userId) {
        //同一层级文件名称唯一
        checkFileName(request.getName(), request.getParentId(), orgId, null);
        String id = IDGenerator.nextStr();
        DashboardModule dashboardModule = new DashboardModule();
        dashboardModule.setId(id);
        dashboardModule.setName(request.getName());
        dashboardModule.setParentId(request.getParentId());
        dashboardModule.setOrganizationId(orgId);
        dashboardModule.setPos(getNextPos(orgId));
        dashboardModule.setCreateTime(System.currentTimeMillis());
        dashboardModule.setUpdateTime(System.currentTimeMillis());
        dashboardModule.setCreateUser(userId);
        dashboardModule.setUpdateUser(userId);
        dashboardModuleMapper.insert(dashboardModule);

        //日志
        OperationLogContext.setContext(LogContextInfo.builder()
                .modifiedValue(dashboardModule)
                .resourceId(id)
                .resourceName(dashboardModule.getName())
                .build());
        return dashboardModule;
    }

    public Long getNextPos(String orgId) {
        Long pos = extDashboardModuleMapper.getNextPosByOrgId(orgId);
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
        if (extDashboardModuleMapper.countByName(name, parentId, orgId, id) > 0) {
            throw new GenericException(Translator.get("dashboard_module_name_exist"));
        }
    }

    /**
     * 重命名文件夹
     *
     * @param request
     * @param userId
     */
    @OperationLog(module = LogModule.DASHBOARD, type = LogType.UPDATE)
    public void rename(DashboardModuleRenameRequest request, String userId) {
        DashboardModule originalDashboardModule = checkDashboardModule(request.getId());
        checkFileName(request.getName(), originalDashboardModule.getParentId(), originalDashboardModule.getOrganizationId(), originalDashboardModule.getId());

        DashboardModule dashboardModule = BeanUtils.copyBean(new DashboardModule(), request);
        dashboardModule.setUpdateTime(System.currentTimeMillis());
        dashboardModule.setName(request.getName());
        dashboardModule.setUpdateUser(userId);
        dashboardModuleMapper.update(dashboardModule);

        // 添加日志上下文
        String resourceName = Optional.ofNullable(dashboardModule.getName()).orElse(originalDashboardModule.getName());
        OperationLogContext.setContext(
                LogContextInfo.builder()
                        .originalValue(originalDashboardModule)
                        .modifiedValue(checkDashboardModule(request.getId()))
                        .resourceId(request.getId())
                        .resourceName(resourceName)
                        .build()
        );
    }

    public DashboardModule checkDashboardModule(String id) {
        DashboardModule dashboardModule = dashboardModuleMapper.selectByPrimaryKey(id);
        if (dashboardModule == null) {
            throw new GenericException(Translator.get("dashboard_module_blank"));
        }
        return dashboardModule;
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
        List<DashboardModule> dashboardModules = dashboardModuleMapper.selectByIds(ids);
        //刪除文件夹
        dashboardModuleMapper.deleteByIds(ids);
        List<LogDTO> logs = new ArrayList<>();
        // 添加日志上下文
        dashboardModules.forEach(dashboardModule -> {
            LogDTO logDTO = new LogDTO(dashboardModule.getOrganizationId(), dashboardModule.getId(), userId, LogType.DELETE, LogModule.DASHBOARD, dashboardModule.getName());
            logDTO.setOriginalValue(dashboardModule);
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
        if (extDashboardModuleMapper.countDashboardByIds(ids, orgId) > 0) {
            throw new GenericException(Translator.get("dashboard_module_cannot_delete"));
        }
    }

    /**
     * 仪表板树结构
     *
     * @param userId
     * @param orgId
     *
     * @return
     */
    public List<DashboardTreeNode> getTree(String userId, String orgId) {
        List<DashboardTreeNode> moduleNode = extDashboardModuleMapper.selectTreeNode(orgId);
        //查询模块下仪表板数据
        List<String> departmentIds = getDepartmentIds(userId, orgId);
        List<DashboardTreeNode> dashboardNode = extDashboardMapper.selectDashboardNode(departmentIds, orgId, userId);

        Set<String> myCollects = new HashSet<>(extDashboardCollectionMapper.getByUserId(userId));
        dashboardNode.forEach(node -> {
            if (myCollects.contains(node.getId())) {
                node.setMyCollect(true);
            }
        });

        dashboardNode.addAll(moduleNode);

        return DashboardTreeNode.buildTree(dashboardNode);
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

    /**
     * 数量
     *
     * @param userId
     * @param orgId
     *
     * @return
     */
    public Map<String, Long> moduleCount(String userId, String orgId) {
        List<DashboardTreeNode> moduleNode = extDashboardModuleMapper.selectTreeNode(orgId);
        //查询模块下仪表板数据
        List<String> departmentIds = getDepartmentIds(userId, orgId);
        List<DashboardTreeNode> dashboardNode = extDashboardMapper.selectDashboardNode(departmentIds, orgId, userId);

        List<DashboardTreeNode> dashboardTreeNodes = DashboardTreeNode.buildTree(moduleNode);

        Map<String, Integer> moduleCountMap = dashboardNode.stream()
                .collect(Collectors.groupingBy(
                        node -> node.getParentId(),
                        Collectors.summingInt(node -> 1)
                ));

        Map<String, Long> countMap = new HashMap<>();
        for (DashboardTreeNode root : dashboardTreeNodes) {
            // 递归计算每个节点的仪表板数量
            calculateDashboardCount(root, countMap, moduleCountMap);
        }

        //我的收藏
        int myCollect = extDashboardCollectionMapper.countMyCollect(userId);
        countMap.put("myCollect", (long) myCollect);
        return countMap;
    }


    private int calculateDashboardCount(BaseTreeNode node, Map<String, Long> countMap, Map<String, Integer> moduleCountMap) {
        int count = 0;

        if (moduleCountMap.containsKey(node.getId())) {
            count += moduleCountMap.get(node.getId());
        }

        if (node.getChildren() != null && !node.getChildren().isEmpty()) {
            for (BaseTreeNode child : node.getChildren()) {
                count += calculateDashboardCount(child, countMap, moduleCountMap);
            }
        }

        countMap.put(node.getId(), (long) count);

        return count;
    }


    /**
     * 仪表板文件夹移动
     *
     * @param request
     * @param userId
     */
    public void moveNode(NodeMoveRequest request, String userId) {
        NodeSortDTO nodeSortDTO = super.getNodeSortDTO(request,
                extDashboardModuleMapper::selectBaseModuleById,
                extDashboardModuleMapper::selectModuleByParentIdAndPosOperator,
                false);


        DashboardModule dashboardModule = new DashboardModule();
        dashboardModule.setParentId(nodeSortDTO.getParent().getId());
        dashboardModule.setId(request.getDragNodeId());
        if (dashboardModuleMapper.countByExample(dashboardModule) == 0) {
            DashboardModule moveModule = dashboardModuleMapper.selectByPrimaryKey(request.getDragNodeId());
            moveModule.setParentId(nodeSortDTO.getParent().getId());
            checkFileName(moveModule.getName(), moveModule.getParentId(), moveModule.getOrganizationId(), null);

            moveModule.setUpdateUser(userId);
            moveModule.setUpdateTime(System.currentTimeMillis());
            dashboardModuleMapper.update(moveModule);
        }

        super.sort(nodeSortDTO);
    }

    @Override
    public void updatePos(String id, long pos) {
        DashboardModule dashboardModule = new DashboardModule();
        dashboardModule.setId(id);
        dashboardModule.setPos(pos);
        dashboardModuleMapper.update(dashboardModule);
    }

    @Override
    public void refreshPos(String parentId) {
        List<String> childrenIds = extDashboardModuleMapper.selectChildrenIds(parentId);
        List<DashboardModule> dashboardModules = new ArrayList<>();
        for (int i = 0; i < childrenIds.size(); i++) {
            String nodeId = childrenIds.get(i);
            DashboardModule updateDashboard = new DashboardModule();
            updateDashboard.setId(nodeId);
            updateDashboard.setPos((i + 1) * LIMIT_POS);
            dashboardModules.add(updateDashboard);
        }
        extDashboardModuleMapper.batchUpdate(dashboardModules);
    }

    /**
     * 获取文件夹名称
     *
     * @param id
     *
     * @return
     */
    public String getDashboardModuleName(String id) {
        DashboardModule dashboardModule = checkDashboardModule(id);
        return dashboardModule.getName();
    }
}
