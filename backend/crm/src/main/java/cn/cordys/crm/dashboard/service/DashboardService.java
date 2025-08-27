package cn.cordys.crm.dashboard.service;

import cn.cordys.aspectj.annotation.OperationLog;
import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.aspectj.dto.LogContextInfo;
import cn.cordys.common.constants.InternalUser;
import cn.cordys.common.dto.BasePageRequest;
import cn.cordys.common.dto.BaseTreeNode;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.Pager;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.NodeSortUtils;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.dashboard.domain.Dashboard;
import cn.cordys.crm.dashboard.domain.DashboardCollection;
import cn.cordys.crm.dashboard.dto.DashboardLogDTO;
import cn.cordys.crm.dashboard.dto.MoveNodeSortDTO;
import cn.cordys.crm.dashboard.dto.request.*;
import cn.cordys.crm.dashboard.dto.response.DashboardDetailResponse;
import cn.cordys.crm.dashboard.dto.response.DashboardPageResponse;
import cn.cordys.crm.dashboard.mapper.ExtDashboardCollectionMapper;
import cn.cordys.crm.dashboard.mapper.ExtDashboardMapper;
import cn.cordys.crm.system.dto.ScopeNameDTO;
import cn.cordys.crm.system.dto.request.NodeMoveRequest;
import cn.cordys.crm.system.mapper.ExtOrganizationUserMapper;
import cn.cordys.crm.system.mapper.ExtUserMapper;
import cn.cordys.crm.system.service.DepartmentService;
import cn.cordys.crm.system.service.UserExtendService;
import cn.cordys.mybatis.BaseMapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
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
public class DashboardService extends DashboardSortService {

    @Resource
    private ExtDashboardMapper extDashboardMapper;
    @Resource
    private BaseMapper<Dashboard> dashboardMapper;
    @Resource
    private UserExtendService userExtendService;
    @Resource
    private DashboardModuleService dashboardModuleService;
    @Resource
    private BaseMapper<DashboardCollection> dashboardCollectionMapper;
    @Resource
    private ExtDashboardCollectionMapper extDashboardCollectionMapper;
    @Resource
    private ExtUserMapper extUserMapper;
    @Resource
    private SqlSessionFactory sqlSessionFactory;
    @Resource
    private ExtOrganizationUserMapper extOrganizationUserMapper;
    @Resource
    private DepartmentService departmentService;

    /**
     * 添加仪表板
     *
     * @param request
     * @param orgId
     * @param userId
     * @return
     */
    @OperationLog(module = LogModule.DASHBOARD, type = LogType.ADD)
    public Dashboard addDashboard(DashboardAddRequest request, String orgId, String userId) {
        checkDashboardName(request.getName(), request.getDashboardModuleId(), orgId, null);
        dashboardModuleService.checkDashboardModule(request.getDashboardModuleId());
        String id = IDGenerator.nextStr();
        Dashboard dashboard = new Dashboard();
        dashboard.setId(id);
        dashboard.setName(request.getName());
        dashboard.setResourceId(request.getResourceId());
        dashboard.setPos(getNextPos(orgId));
        dashboard.setOrganizationId(orgId);
        dashboard.setDashboardModuleId(request.getDashboardModuleId());
        dashboard.setScopeId(JSON.toJSONString(request.getScopeIds()));
        dashboard.setDescription(request.getDescription());
        dashboard.setCreateTime(System.currentTimeMillis());
        dashboard.setUpdateTime(System.currentTimeMillis());
        dashboard.setCreateUser(userId);
        dashboard.setUpdateUser(userId);

        dashboardMapper.insert(dashboard);

        //日志
        OperationLogContext.setContext(LogContextInfo.builder()
                .modifiedValue(dashboard)
                .resourceId(id)
                .resourceName(dashboard.getName())
                .build());
        return dashboard;
    }

    private Long getNextPos(String orgId) {
        Long pos = extDashboardMapper.getNextPosByOrgId(orgId);
        return (pos == null ? 0 : pos) + NodeSortUtils.DEFAULT_NODE_INTERVAL_POS;
    }


    private void checkDashboardName(String name, String dashboardModuleId, String orgId, String id) {
        if (extDashboardMapper.countByName(name, dashboardModuleId, orgId, id) > 0) {
            throw new GenericException(Translator.get("dashboard_name_exist"));
        }
    }


    /**
     * 仪表板详情
     *
     * @param id
     * @return
     */
    public DashboardDetailResponse getDashboardDetail(String id) {
        DashboardDetailResponse response = extDashboardMapper.getDetail(id);
        if (response != null && !StringUtils.isBlank(response.getScopeId())) {
            response.setMembers(userExtendService.getScope(JSON.parseArray(response.getScopeId(), String.class)));
        }
        return response;
    }

    /**
     * 更新仪表板
     *
     * @param request
     * @param orgId
     * @param userId
     */
    @OperationLog(module = LogModule.DASHBOARD, type = LogType.UPDATE)
    public void updateDashboard(DashboardUpdateRequest request, String orgId, String userId) {
        checkDashboardName(request.getName(), request.getDashboardModuleId(), orgId, request.getId());
        Dashboard originalDashboard = checkDashboard(request.getId());
        DashboardLogDTO originalDetail = getLogDetail(originalDashboard.getId());

        Dashboard dashboard = new Dashboard();
        BeanUtils.copyBean(dashboard, request);
        dashboard.setScopeId(JSON.toJSONString(request.getScopeIds()));
        dashboard.setUpdateTime(System.currentTimeMillis());
        dashboard.setUpdateUser(userId);
        dashboardMapper.update(dashboard);

        DashboardLogDTO newDetail = getLogDetail(originalDashboard.getId());

        // 添加日志上下文
        String resourceName = Optional.ofNullable(dashboard.getName()).orElse(originalDashboard.getName());
        OperationLogContext.setContext(
                LogContextInfo.builder()
                        .originalValue(originalDetail)
                        .modifiedValue(newDetail)
                        .resourceId(request.getId())
                        .resourceName(resourceName)
                        .build()
        );
    }

    private DashboardLogDTO getLogDetail(String id) {
        DashboardDetailResponse dashboardDetail = getDashboardDetail(id);
        DashboardLogDTO detailDTO = new DashboardLogDTO();
        BeanUtils.copyBean(detailDTO, dashboardDetail);
        List<String> names = dashboardDetail.getMembers().stream().map(ScopeNameDTO::getName).toList();
        detailDTO.setMembers(names);
        return detailDTO;
    }

    private Dashboard checkDashboard(String id) {
        Dashboard dashboard = dashboardMapper.selectByPrimaryKey(id);
        if (dashboard == null) {
            throw new GenericException(Translator.get("dashboard_blank"));
        }
        return dashboard;
    }


    /**
     * 重命名仪表板
     *
     * @param request
     * @param userId
     */
    @OperationLog(module = LogModule.DASHBOARD, type = LogType.UPDATE)
    public void rename(DashboardRenameRequest request, String userId, String orgId) {
        Dashboard originalDashboard = checkDashboard(request.getId());
        checkDashboardName(request.getName(), request.getDashboardModuleId(), orgId, request.getId());

        Dashboard dashboard = BeanUtils.copyBean(new Dashboard(), request);
        dashboard.setUpdateTime(System.currentTimeMillis());
        dashboard.setName(request.getName());
        dashboard.setUpdateUser(userId);
        dashboardMapper.update(dashboard);

        // 添加日志上下文
        String resourceName = Optional.ofNullable(dashboard.getName()).orElse(originalDashboard.getName());
        OperationLogContext.setContext(
                LogContextInfo.builder()
                        .originalValue(originalDashboard)
                        .modifiedValue(checkDashboard(request.getId()))
                        .resourceId(request.getId())
                        .resourceName(resourceName)
                        .build()
        );
    }


    /**
     * 删除仪表板
     *
     * @param id
     */
    @OperationLog(module = LogModule.DASHBOARD, type = LogType.DELETE, resourceId = "{#id}")
    public void delete(String id) {
        Dashboard dashboard = checkDashboard(id);
        dashboardMapper.deleteByPrimaryKey(id);
        extDashboardCollectionMapper.deleteByDashboardId(id);

        // 设置操作对象
        OperationLogContext.setResourceName(dashboard.getName());
    }


    /**
     * 列表查询
     *
     * @param request
     * @param userId
     * @param orgId
     * @return
     */
    public Pager<List<DashboardPageResponse>> getList(DashboardPageRequest request, String userId, String orgId) {
        List<String> departmentIds = new ArrayList<>();
        if (!Strings.CI.equals(userId, InternalUser.ADMIN.getValue())) {
            String departmentId = extOrganizationUserMapper.getDepartmentByUserId(userId);
            List<BaseTreeNode> departmentTree = departmentService.getTree(orgId);
            departmentIds = dashboardModuleService.getParentIds(departmentTree, departmentId);
        }


        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        List<DashboardPageResponse> dashboardList = extDashboardMapper.list(request, userId, orgId, departmentIds);
        handleData(dashboardList, userId);
        return PageUtils.setPageInfo(page, dashboardList);
    }

    /**
     * 数据处理
     *
     * @param dashboardList
     */
    private void handleData(List<DashboardPageResponse> dashboardList, String userId) {
        if (CollectionUtils.isNotEmpty(dashboardList)) {
            //创建人 更新人
            List<String> ids = new ArrayList<>();
            ids.addAll(dashboardList.stream().map(DashboardPageResponse::getCreateUser).toList());
            ids.addAll(dashboardList.stream().map(DashboardPageResponse::getUpdateUser).toList());
            List<OptionDTO> optionDTOS = extUserMapper.selectUserOptionByIds(ids);
            Map<String, String> userMap = optionDTOS
                    .stream()
                    .collect(Collectors.toMap(OptionDTO::getId, OptionDTO::getName));

            Set<String> myCollects = new HashSet<>(extDashboardCollectionMapper.getByUserId(userId));

            dashboardList.forEach(dashboard -> {
                dashboard.setMembers(userExtendService.getScope(JSON.parseArray(dashboard.getScopeId(), String.class)));
                if (userMap.containsKey(dashboard.getCreateUser())) {
                    dashboard.setCreateUserName(userMap.get(dashboard.getCreateUser()));
                }
                if (userMap.containsKey(dashboard.getUpdateUser())) {
                    dashboard.setUpdateUserName(userMap.get(dashboard.getUpdateUser()));
                }
                if (myCollects.contains(dashboard.getId())) {
                    dashboard.setMyCollect(true);
                }
            });
        }
    }


    /**
     * 收藏仪表板
     *
     * @param id
     * @param userId
     */
    public void collect(String id, String userId) {
        checkCollect(id, userId);
        DashboardCollection dashboardCollection = new DashboardCollection();
        dashboardCollection.setId(IDGenerator.nextStr());
        dashboardCollection.setDashboardId(id);
        dashboardCollection.setUserId(userId);
        dashboardCollection.setCreateTime(System.currentTimeMillis());
        dashboardCollection.setUpdateTime(System.currentTimeMillis());
        dashboardCollection.setCreateUser(userId);
        dashboardCollection.setUpdateUser(userId);
        dashboardCollectionMapper.insert(dashboardCollection);
    }

    private void checkCollect(String id, String userId) {
        if (extDashboardCollectionMapper.checkCollect(id, userId) > 0) {
            throw new GenericException(Translator.get("dashboard_collect_exist"));
        }
    }


    /**
     * 我的收藏列表
     *
     * @param request
     * @param userId
     * @param orgId
     * @return
     */
    public List<DashboardPageResponse> collectList(BasePageRequest request, String userId, String orgId) {
        List<DashboardPageResponse> dashboardList = extDashboardCollectionMapper.collectList(request, userId, orgId);
        handleData(dashboardList, userId);
        return dashboardList;
    }

    public void unCollect(String dashboardId, String userId) {
        extDashboardCollectionMapper.unCollect(dashboardId, userId);
    }


    /**
     * 仪表板排序
     *
     * @param request
     * @param userId
     * @param orgId
     */
    public void editPos(DashboardEditPosRequest request, String userId, String orgId) {
        Dashboard dashboard = checkDashboard(request.getMoveId());
        if (!Strings.CS.equals(request.getDashboardModuleId(), dashboard.getDashboardModuleId())) {
            dashboardModuleService.checkDashboardModule(request.getDashboardModuleId());
            checkDashboardName(dashboard.getName(), request.getDashboardModuleId(), orgId, dashboard.getId());

            dashboard.setDashboardModuleId(request.getDashboardModuleId());
            dashboard.setUpdateUser(userId);
            dashboard.setUpdateTime(System.currentTimeMillis());
            dashboardMapper.update(dashboard);
        }
        if (Strings.CS.equals(request.getTargetId(), request.getMoveId())) {
            return;
        }
        moveNode(request, orgId);
    }


    public void moveNode(DashboardEditPosRequest posRequest, String orgId) {
        NodeMoveRequest request = super.getNodeMoveRequest(posRequest, true);
        MoveNodeSortDTO sortDTO = super.getNodeSortDTO(
                orgId,
                request,
                extDashboardMapper::selectDragInfoById,
                extDashboardMapper::selectNodeByPosOperator
        );
        super.sort(sortDTO);
    }


    @Override
    public void updatePos(String id, long pos) {
        extDashboardMapper.updatePos(id, pos);
    }

    @Override
    public void refreshPos(String orgId) {
        List<String> posIds = extDashboardMapper.selectIdByOrgIdOrderByPos(orgId);
        SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH);
        ExtDashboardMapper batchUpdateMapper = sqlSession.getMapper(ExtDashboardMapper.class);
        for (int i = 0; i < posIds.size(); i++) {
            batchUpdateMapper.updatePos(posIds.get(i), i * NodeSortUtils.DEFAULT_NODE_INTERVAL_POS);
        }
        sqlSession.flushStatements();
        SqlSessionUtils.closeSqlSession(sqlSession, sqlSessionFactory);
    }
}
