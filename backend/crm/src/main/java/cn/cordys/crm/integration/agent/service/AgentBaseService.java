package cn.cordys.crm.integration.agent.service;

import cn.cordys.aspectj.annotation.OperationLog;
import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.aspectj.dto.LogContextInfo;
import cn.cordys.common.constants.InternalUser;
import cn.cordys.common.constants.ThirdConfigTypeConstants;
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
import cn.cordys.crm.dashboard.dto.MoveNodeSortDTO;
import cn.cordys.crm.dashboard.service.DashboardSortService;
import cn.cordys.crm.integration.agent.constant.MaxKBApiPaths;
import cn.cordys.crm.integration.agent.domain.Agent;
import cn.cordys.crm.integration.agent.domain.AgentCollection;
import cn.cordys.crm.integration.agent.dto.AgentLogDTO;
import cn.cordys.crm.integration.agent.dto.AgentOptionDTO;
import cn.cordys.crm.integration.agent.dto.ParameterDTO;
import cn.cordys.crm.integration.agent.dto.request.*;
import cn.cordys.crm.integration.agent.dto.response.AgentDetailResponse;
import cn.cordys.crm.integration.agent.dto.response.AgentPageResponse;
import cn.cordys.crm.integration.agent.dto.response.ScriptResponse;
import cn.cordys.crm.integration.agent.mapper.ExtAgentCollectionMapper;
import cn.cordys.crm.integration.agent.mapper.ExtAgentMapper;
import cn.cordys.crm.integration.agent.response.*;
import cn.cordys.crm.integration.common.client.QrCodeClient;
import cn.cordys.crm.integration.common.dto.ThirdConfigBaseDTO;
import cn.cordys.crm.integration.common.request.MaxKBThirdConfigRequest;
import cn.cordys.crm.integration.common.utils.HttpRequestUtil;
import cn.cordys.crm.system.constants.OrganizationConfigConstants;
import cn.cordys.crm.system.domain.OrganizationConfig;
import cn.cordys.crm.system.domain.OrganizationConfigDetail;
import cn.cordys.crm.system.dto.ScopeNameDTO;
import cn.cordys.crm.system.dto.request.NodeMoveRequest;
import cn.cordys.crm.system.mapper.ExtOrganizationConfigDetailMapper;
import cn.cordys.crm.system.mapper.ExtOrganizationConfigMapper;
import cn.cordys.crm.system.mapper.ExtOrganizationUserMapper;
import cn.cordys.crm.system.mapper.ExtUserMapper;
import cn.cordys.crm.system.service.DepartmentService;
import cn.cordys.crm.system.service.UserExtendService;
import cn.cordys.mybatis.BaseMapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = Exception.class)
public class AgentBaseService extends DashboardSortService {

  @Resource private ExtAgentMapper extAgentMapper;
  @Resource private AgentModuleService agentModuleService;
  @Resource private BaseMapper<Agent> agentMapper;
  @Resource private UserExtendService userExtendService;
  @Resource private ExtAgentCollectionMapper extAgentCollectionMapper;
  @Resource private ExtOrganizationUserMapper extOrganizationUserMapper;
  @Resource private DepartmentService departmentService;
  @Resource private ExtUserMapper extUserMapper;
  @Resource private BaseMapper<AgentCollection> agentCollectionMapper;
  @Resource private SqlSessionFactory sqlSessionFactory;
  @Resource private ExtOrganizationConfigMapper extOrganizationConfigMapper;
  @Resource private ExtOrganizationConfigDetailMapper extOrganizationConfigDetailMapper;
  @Resource private QrCodeClient qrCodeClient;

  /**
   * 添加智能体
   *
   * @param request
   * @param orgId
   * @param userId
   * @return
   */
  @OperationLog(module = LogModule.AGENT, type = LogType.ADD)
  public Agent addAgent(AgentAddRequest request, String orgId, String userId) {
    checkAgentName(request.getName(), request.getAgentModuleId(), orgId, null);
    agentModuleService.checkAgentModule(request.getAgentModuleId());
    String id = IDGenerator.nextStr();
    Agent agent = new Agent();
    agent.setId(id);
    agent.setName(request.getName());
    agent.setAgentModuleId(request.getAgentModuleId());
    agent.setOrganizationId(orgId);
    agent.setPos(getNextPos(orgId));
    agent.setScopeId(JSON.toJSONString(request.getScopeIds()));
    agent.setScript(request.getScript());
    agent.setDescription(request.getDescription());
    agent.setCreateTime(System.currentTimeMillis());
    agent.setCreateUser(userId);
    agent.setUpdateTime(System.currentTimeMillis());
    agent.setUpdateUser(userId);
    agent.setType(request.getType());
    agent.setWorkspaceId(request.getWorkspaceId());
    agent.setApplicationId(request.getApplicationId());
    agentMapper.insert(agent);

    // 日志
    OperationLogContext.setContext(
        LogContextInfo.builder()
            .modifiedValue(agent)
            .resourceId(id)
            .resourceName(agent.getName())
            .build());

    return agent;
  }

  /**
   * 同一文件下智能体名称唯一
   *
   * @param name
   * @param agentModuleId
   * @param orgId
   * @param id
   */
  private void checkAgentName(String name, String agentModuleId, String orgId, String id) {
    if (extAgentMapper.countByName(name, agentModuleId, orgId, id) > 0) {
      throw new GenericException(Translator.get("agent_name_exist"));
    }
  }

  private Long getNextPos(String orgId) {
    Long pos = extAgentMapper.getNextPosByOrgId(orgId);
    return (pos == null ? 0 : pos) + NodeSortUtils.DEFAULT_NODE_INTERVAL_POS;
  }

  /**
   * 智能体详情
   *
   * @param id
   * @return
   */
  public AgentDetailResponse getDetail(String id) {
    AgentDetailResponse response = extAgentMapper.getDetail(id);
    if (response != null && !StringUtils.isBlank(response.getScopeId())) {
      response.setMembers(
          userExtendService.getScope(JSON.parseArray(response.getScopeId(), String.class)));
    }
    return response;
  }

  /**
   * 更新智能体
   *
   * @param request
   * @param orgId
   * @param userId
   */
  @OperationLog(module = LogModule.AGENT, type = LogType.UPDATE)
  public void updateAgent(AgentUpdateRequest request, String orgId, String userId) {
    checkAgentName(request.getName(), request.getAgentModuleId(), orgId, request.getId());
    Agent originalAgent = checkAgent(request.getId());
    AgentLogDTO originalDetail = getLogDetail(originalAgent.getId());

    Agent agent = new Agent();
    BeanUtils.copyBean(agent, request);
    agent.setScopeId(JSON.toJSONString(request.getScopeIds()));
    agent.setUpdateTime(System.currentTimeMillis());
    agent.setUpdateUser(userId);
    agentMapper.update(agent);

    AgentLogDTO newDetail = getLogDetail(originalAgent.getId());

    // 添加日志上下文
    String resourceName = Optional.ofNullable(agent.getName()).orElse(originalAgent.getName());
    OperationLogContext.setContext(
        LogContextInfo.builder()
            .originalValue(originalDetail)
            .modifiedValue(newDetail)
            .resourceId(request.getId())
            .resourceName(resourceName)
            .build());
  }

  private AgentLogDTO getLogDetail(String id) {
    AgentDetailResponse detail = getDetail(id);
    AgentLogDTO detailDTO = new AgentLogDTO();
    BeanUtils.copyBean(detailDTO, detail);
    List<String> names = detail.getMembers().stream().map(ScopeNameDTO::getName).toList();
    detailDTO.setMembers(names);
    return detailDTO;
  }

  private Agent checkAgent(String id) {
    Agent agent = agentMapper.selectByPrimaryKey(id);
    if (agent == null) {
      throw new GenericException(Translator.get("agent_blank"));
    }
    return agent;
  }

  /**
   * 重命名智能体
   *
   * @param request
   * @param userId
   * @param orgId
   */
  @OperationLog(module = LogModule.AGENT, type = LogType.UPDATE)
  public void rename(AgentRenameRequest request, String userId, String orgId) {
    Agent originalAgent = checkAgent(request.getId());
    checkAgentName(request.getName(), request.getAgentModuleId(), orgId, request.getId());
    Agent agent = BeanUtils.copyBean(new Agent(), request);
    agent.setUpdateTime(System.currentTimeMillis());
    agent.setName(request.getName());
    agent.setUpdateUser(userId);
    agentMapper.update(agent);

    // 添加日志上下文
    String resourceName = Optional.ofNullable(agent.getName()).orElse(originalAgent.getName());
    OperationLogContext.setContext(
        LogContextInfo.builder()
            .originalValue(originalAgent)
            .modifiedValue(checkAgent(request.getId()))
            .resourceId(request.getId())
            .resourceName(resourceName)
            .build());
  }

  /**
   * 删除智能体
   *
   * @param id
   */
  @OperationLog(module = LogModule.AGENT, type = LogType.DELETE, resourceId = "{#id}")
  public void delete(String id) {
    Agent agent = checkAgent(id);
    agentMapper.deleteByPrimaryKey(id);
    extAgentCollectionMapper.deleteByAgentId(id);

    // 设置操作对象
    OperationLogContext.setResourceName(agent.getName());
  }

  /**
   * 智能体列表
   *
   * @param request
   * @param userId
   * @param orgId
   * @return
   */
  public Pager<List<AgentPageResponse>> getList(
      AgentPageRequest request, String userId, String orgId) {
    List<String> departmentIds = new ArrayList<>();
    if (!Strings.CI.equals(userId, InternalUser.ADMIN.getValue())) {
      String departmentId = extOrganizationUserMapper.getDepartmentByUserId(userId);
      List<BaseTreeNode> departmentTree = departmentService.getTree(orgId);
      departmentIds = agentModuleService.getParentIds(departmentTree, departmentId);
    }

    Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
    List<AgentPageResponse> agentList = extAgentMapper.list(request, userId, orgId, departmentIds);
    handleData(agentList, userId);
    return PageUtils.setPageInfo(page, agentList);
  }

  /**
   * 数据处理
   *
   * @param agentList
   * @param userId
   */
  private void handleData(List<AgentPageResponse> agentList, String userId) {
    if (CollectionUtils.isNotEmpty(agentList)) {
      // 创建人 更新人
      List<String> ids = new ArrayList<>();
      ids.addAll(agentList.stream().map(AgentPageResponse::getCreateUser).toList());
      ids.addAll(agentList.stream().map(AgentPageResponse::getUpdateUser).toList());
      List<OptionDTO> options = extUserMapper.selectUserOptionByIds(ids);
      Map<String, String> userMap =
          options.stream().collect(Collectors.toMap(OptionDTO::getId, OptionDTO::getName));

      Set<String> myCollects = new HashSet<>(extAgentCollectionMapper.getByUserId(userId));

      agentList.forEach(
          agent -> {
            agent.setMembers(
                userExtendService.getScope(JSON.parseArray(agent.getScopeId(), String.class)));
            if (userMap.containsKey(agent.getCreateUser())) {
              agent.setCreateUserName(userMap.get(agent.getCreateUser()));
            }
            if (userMap.containsKey(agent.getUpdateUser())) {
              agent.setUpdateUserName(userMap.get(agent.getUpdateUser()));
            }
            if (myCollects.contains(agent.getId())) {
              agent.setMyCollect(true);
            }
          });
    }
  }

  /**
   * 收藏
   *
   * @param id
   * @param userId
   */
  public void collect(String id, String userId) {
    checkCollect(id, userId);
    AgentCollection agentCollection = new AgentCollection();
    agentCollection.setId(IDGenerator.nextStr());
    agentCollection.setAgentId(id);
    agentCollection.setUserId(userId);
    agentCollection.setCreateTime(System.currentTimeMillis());
    agentCollection.setUpdateTime(System.currentTimeMillis());
    agentCollection.setCreateUser(userId);
    agentCollection.setUpdateUser(userId);
    agentCollectionMapper.insert(agentCollection);
  }

  private void checkCollect(String id, String userId) {
    if (extAgentCollectionMapper.checkCollect(id, userId) > 0) {
      throw new GenericException(Translator.get("agent_collect_exist"));
    }
  }

  /**
   * 取消收藏
   *
   * @param agentId
   * @param userId
   */
  public void unCollect(String agentId, String userId) {
    extAgentCollectionMapper.unCollect(agentId, userId);
  }

  /**
   * 收藏列表
   *
   * @param request
   * @param userId
   * @param orgId
   * @return
   */
  public List<AgentPageResponse> collectList(BasePageRequest request, String userId, String orgId) {
    List<AgentPageResponse> agentList =
        extAgentCollectionMapper.collectList(request, userId, orgId);
    handleData(agentList, userId);
    return agentList;
  }

  /**
   * 拖拽排序
   *
   * @param request
   * @param userId
   * @param orgId
   */
  public void editPos(AgentEditPosRequest request, String userId, String orgId) {
    Agent agent = checkAgent(request.getMoveId());
    if (StringUtils.isNotBlank(request.getAgentModuleId())
        && !Strings.CS.equals(request.getAgentModuleId(), agent.getAgentModuleId())) {
      agentModuleService.checkAgentModule(request.getAgentModuleId());
      checkAgentName(agent.getName(), request.getAgentModuleId(), orgId, agent.getId());

      agent.setAgentModuleId(request.getAgentModuleId());
      agent.setUpdateUser(userId);
      agent.setUpdateTime(System.currentTimeMillis());
      agentMapper.update(agent);
    }
    if (Strings.CS.equals(request.getTargetId(), request.getMoveId())) {
      return;
    }
    moveNode(request, orgId);
  }

  public void moveNode(AgentEditPosRequest posRequest, String orgId) {
    NodeMoveRequest request =
        super.getNodeMoveRequest(
            posRequest.getMoveId(), posRequest.getTargetId(), posRequest.getMoveMode(), true);
    MoveNodeSortDTO sortDTO =
        super.getNodeSortDTO(
            orgId,
            request,
            extAgentMapper::selectDragInfoById,
            extAgentMapper::selectNodeByPosOperator);
    super.sort(sortDTO);
  }

  @Override
  public void updatePos(String id, long pos) {
    extAgentMapper.updatePos(id, pos);
  }

  @Override
  public void refreshPos(String orgId) {
    List<String> posIds = extAgentMapper.selectIdByOrgIdOrderByPos(orgId);
    SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH);
    ExtAgentMapper batchUpdateMapper = sqlSession.getMapper(ExtAgentMapper.class);
    for (int i = 0; i < posIds.size(); i++) {
      batchUpdateMapper.updatePos(posIds.get(i), i * NodeSortUtils.DEFAULT_NODE_INTERVAL_POS);
    }
    sqlSession.flushStatements();
    SqlSessionUtils.closeSqlSession(sqlSession, sqlSessionFactory);
  }

  /**
   * 获取用户可选智能体选项
   *
   * @param userId
   * @param orgId
   * @return
   */
  public List<AgentOptionDTO> getAgentOptions(String userId, String orgId) {
    List<String> departmentIds = new ArrayList<>();
    if (!Strings.CI.equals(userId, InternalUser.ADMIN.getValue())) {
      String departmentId = extOrganizationUserMapper.getDepartmentByUserId(userId);
      List<BaseTreeNode> departmentTree = departmentService.getTree(orgId);
      departmentIds = agentModuleService.getParentIds(departmentTree, departmentId);
    }
    return extAgentMapper.getOptions(userId, orgId, departmentIds);
  }

  /**
   * 检测配置连接
   *
   * @param orgId
   * @return
   */
  public Boolean checkConfig(String orgId) {
    var configurationDTO = getConfig(orgId);
    if (configurationDTO == null) {
      return false;
    }
    return BooleanUtils.isTrue(configurationDTO.getVerify());
  }

  /**
   * 获取配置
   *
   * @param orgId
   * @return
   */
  private ThirdConfigBaseDTO<?> getConfig(String orgId) {
    OrganizationConfig organizationConfig =
        extOrganizationConfigMapper.getOrganizationConfig(
            orgId, OrganizationConfigConstants.ConfigType.THIRD.name());
    if (organizationConfig == null) {
      return null;
    }
    List<OrganizationConfigDetail> details =
        extOrganizationConfigDetailMapper.getOrgConfigDetailByType(
            organizationConfig.getId(), null, List.of(ThirdConfigTypeConstants.MAXKB.name()));

    if (CollectionUtils.isEmpty(details)) {
      return null;
    }

    var thirdConfigBaseDTO =
        JSON.parseObject(new String(details.getFirst().getContent()), ThirdConfigBaseDTO.class);
    MaxKBThirdConfigRequest maxKBThirdConfigRequest;
    if (thirdConfigBaseDTO.getConfig() == null) {
      maxKBThirdConfigRequest =
          JSON.parseObject(
              new String(details.getFirst().getContent()), MaxKBThirdConfigRequest.class);
    } else {
      maxKBThirdConfigRequest =
          JSON.MAPPER.convertValue(thirdConfigBaseDTO.getConfig(), MaxKBThirdConfigRequest.class);
    }

    thirdConfigBaseDTO.setConfig(maxKBThirdConfigRequest);
    return thirdConfigBaseDTO;
  }

  /**
   * 获取工作空间
   *
   * @param orgId
   * @return
   */
  public List<OptionDTO> workspace(String orgId) {
    ThirdConfigBaseDTO<?> config = getConfig(orgId);
    if (config == null) {
      return Collections.emptyList();
    }
    return getWorkspace(config);
  }

  private List<OptionDTO> getWorkspace(ThirdConfigBaseDTO<?> baseConfig) {
    MaxKBThirdConfigRequest config =
        JSON.MAPPER.convertValue(baseConfig.getConfig(), MaxKBThirdConfigRequest.class);
    String body =
        qrCodeClient.exchange(
            config.getMkAddress().concat(MaxKBApiPaths.WORKSPACE),
            "Bearer " + config.getAppSecret(),
            HttpHeaders.AUTHORIZATION,
            MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_JSON);
    MaxKBDataResponse entity = JSON.parseObject(body, MaxKBDataResponse.class);
    if (entity.getCode() != 200) {
      throw new GenericException("获取工作空间失败，错误信息：" + entity.getMessage());
    }
    return entity.getData();
  }

  /**
   * 获取智能体应用列表
   *
   * @param workspaceId
   * @param orgId
   * @return
   */
  public List<OptionDTO> application(String workspaceId, String orgId) {
    ThirdConfigBaseDTO<?> config = getConfig(orgId);
    if (config == null) {
      return Collections.emptyList();
    }
    return getApplication(workspaceId, config);
  }

  private List<OptionDTO> getApplication(String workspaceId, ThirdConfigBaseDTO<?> baseConfig) {
    MaxKBThirdConfigRequest config =
        JSON.MAPPER.convertValue(baseConfig.getConfig(), MaxKBThirdConfigRequest.class);
    String body =
        qrCodeClient.exchange(
            HttpRequestUtil.urlTransfer(
                config.getMkAddress().concat(MaxKBApiPaths.APPLICATION), workspaceId),
            "Bearer " + config.getAppSecret(),
            HttpHeaders.AUTHORIZATION,
            MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_JSON);
    MaxKBDataResponse entity = JSON.parseObject(body, MaxKBDataResponse.class);
    if (entity.getCode() != 200) {
      throw new GenericException("获取智能体应用失败，错误信息：" + entity.getMessage());
    }
    return entity.getData();
  }

  /**
   * 获取脚本信息
   *
   * @param request
   * @param orgId
   * @return
   */
  public ScriptResponse script(ScriptRequest request, String orgId) {
    ThirdConfigBaseDTO<?> config = getConfig(orgId);
    if (config == null) {
      return new ScriptResponse();
    }
    return getScript(request, config);
  }

  private ScriptResponse getScript(ScriptRequest request, ThirdConfigBaseDTO<?> baseConfig) {
    MaxKBThirdConfigRequest config =
        JSON.MAPPER.convertValue(baseConfig.getConfig(), MaxKBThirdConfigRequest.class);
    ScriptResponse response = new ScriptResponse();
    List<ParameterDTO> parameters = new ArrayList<>();
    String accessToken =
        qrCodeClient.exchange(
            HttpRequestUtil.urlTransfer(
                config.getMkAddress().concat(MaxKBApiPaths.ACCESS_TOKEN),
                request.getWorkspaceId(),
                request.getApplicationId()),
            "Bearer " + config.getAppSecret(),
            HttpHeaders.AUTHORIZATION,
            MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_JSON);
    MaxKBTokenResponse maxKBResponse = JSON.parseObject(accessToken, MaxKBTokenResponse.class);
    if (maxKBResponse.getCode() != 200) {
      throw new GenericException("获取accessToken失败，错误信息：" + maxKBResponse.getMessage());
    }
    response.setSrc(
        config.getMkAddress().concat("/chat/").concat(maxKBResponse.getData().getAccessToken()));

    String application =
        qrCodeClient.exchange(
            HttpRequestUtil.urlTransfer(
                config.getMkAddress().concat(MaxKBApiPaths.APPLICATION_DETAIL),
                request.getWorkspaceId(),
                request.getApplicationId()),
            "Bearer " + config.getAppSecret(),
            HttpHeaders.AUTHORIZATION,
            MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_JSON);
    MaxKBApplicationResponse applicationResponse =
        JSON.parseObject(application, MaxKBApplicationResponse.class);
    if (applicationResponse.getCode() != 200) {
      throw new GenericException("获取应用信息失败，错误信息：" + applicationResponse.getMessage());
    }

    if (CollectionUtils.isNotEmpty(applicationResponse.getData().getWorkflow().getNodes())) {
      List<Nodes> nodes = applicationResponse.getData().getWorkflow().getNodes();
      Nodes baseNode =
          nodes.stream()
              .filter(node -> Strings.CI.equals(node.getId(), "base-node"))
              .toList()
              .getFirst();
      if (baseNode != null) {
        List<ApiInputFieldList> apiInputFieldList = baseNode.getProperties().getApiInputFieldList();
        if (CollectionUtils.isNotEmpty(apiInputFieldList)) {
          apiInputFieldList.forEach(
              field -> {
                ParameterDTO parameterDTO = new ParameterDTO();
                parameterDTO.setParameter(field.getVariable());
                parameterDTO.setValue(field.getDefaultValue());
                parameters.add(parameterDTO);
              });
        }
      }
    }
    response.setParameters(parameters);
    return response;
  }

  /**
   * 获取版本
   *
   * @param orgId
   * @return
   */
  public String edition(String orgId) {
    ThirdConfigBaseDTO<?> config = getConfig(orgId);
    if (config == null) {
      throw new GenericException(Translator.get("third.config.not.exist"));
    }
    return getEdition(config);
  }

  private String getEdition(ThirdConfigBaseDTO<?> baseConfig) {
    MaxKBThirdConfigRequest config =
        JSON.MAPPER.convertValue(baseConfig.getConfig(), MaxKBThirdConfigRequest.class);
    String body =
        qrCodeClient.exchange(
            config.getMkAddress().concat(MaxKBApiPaths.EDITION),
            "Bearer " + config.getAppSecret(),
            HttpHeaders.AUTHORIZATION,
            MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_JSON);
    var map = JSON.parseObject(body, Map.class);
    if ((Integer) map.get("code") != 200) {
      throw new GenericException("获取版本异常，错误信息：" + map.get("message"));
    }

    var dataMap = (Map) map.get("data");
    return (String) dataMap.get("edition");
  }
}
