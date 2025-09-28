package cn.cordys.crm.integration.wecom.service;

import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.dto.LogDTO;
import cn.cordys.common.constants.DepartmentConstants;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.LogUtils;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.integration.auth.dto.ThirdConfigurationDTO;
import cn.cordys.crm.integration.sso.service.TokenService;
import cn.cordys.crm.integration.wecom.constant.WeComApiPaths;
import cn.cordys.crm.integration.wecom.dto.WeComDepartment;
import cn.cordys.crm.integration.wecom.dto.WeComUser;
import cn.cordys.crm.integration.wecom.response.DepartmentListResponse;
import cn.cordys.crm.integration.wecom.response.UserListResponse;
import cn.cordys.crm.integration.wecom.utils.DataHandleUtils;
import cn.cordys.crm.integration.wecom.utils.HttpRequestUtil;
import cn.cordys.crm.system.constants.OrganizationConfigConstants;
import cn.cordys.crm.system.service.IntegrationConfigService;
import cn.cordys.crm.system.service.LogService;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Transactional(rollbackFor = Exception.class)
public class WeComDepartmentService {

    private static final String LOCK_PREFIX = "orgId_sync_";
    private static final String DEPT_TREE_CACHE_KEY = "dept_tree_cache::";
    private static final String PERMISSION_CACHE_PATTERN = "permission_cache*%s";
    private static final int SCAN_COUNT = 100;

    @Resource
    private TokenService tokenService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private IntegrationConfigService integrationConfigService;

    /**
     * 企业微信同步组织架构
     *
     * @param operatorId 操作人ID
     * @param orgId      组织ID
     */
    public void syncUser(String operatorId, String orgId) {
        Redisson redisson = CommonBeanFactory.getBean(Redisson.class);
        assert redisson != null;
        RLock lock = redisson.getLock(LOCK_PREFIX + orgId);

        // 尝试获取锁，避免并发同步
        if (!lock.tryLock()) {
            throw new GenericException("当前正在执行同步任务！");
        }

        try {
            performSync(operatorId, orgId);
            clearCaches(orgId);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 执行同步操作
     */
    private void performSync(String operatorId, String orgId) {
        LogService logService = CommonBeanFactory.getBean(LogService.class);

        // 获取企业微信配置信息
        ThirdConfigurationDTO weComConfig = getWeComConfig(orgId);
        boolean syncStatus = integrationConfigService.getSyncStatus(orgId,
                OrganizationConfigConstants.ConfigType.THIRD.name(),
                DepartmentConstants.WECOM.name());

        // 获取访问令牌
        String accessToken = tokenService.getAssessToken(weComConfig.getCorpId(), weComConfig.getAppSecret());
        if (StringUtils.isBlank(accessToken)) {
            throw new GenericException("获取企业微信访问令牌失败");
        }

        // 获取部门和用户数据
        List<WeComDepartment> departments = getDepartmentList(accessToken);
        List<Long> departmentIds = departments.stream().map(WeComDepartment::getId).toList();
        Map<String, List<WeComUser>> departmentUserMap = getDepartmentUser(accessToken, departmentIds);

        // 处理同步数据
        syncDepartmentsAndUsers(orgId, departments, departmentUserMap, syncStatus, operatorId);

        // 记录日志
        assert logService != null;
        logSyncOperation(logService, orgId, operatorId);
    }

    /**
     * 获取企业微信配置
     */
    private ThirdConfigurationDTO getWeComConfig(String orgId) {
        List<ThirdConfigurationDTO> configs = integrationConfigService.getThirdConfig(orgId);
        if (CollectionUtils.isEmpty(configs)) {
            throw new GenericException("未配置企业信息");
        }

        return configs.stream()
                .filter(config -> Strings.CI.equals(config.getType(), DepartmentConstants.WECOM.name()))
                .findFirst()
                .orElseThrow(() -> new GenericException("未配置企业微信信息"));
    }

    /**
     * 同步部门和用户数据
     */
    private void syncDepartmentsAndUsers(String orgId, List<WeComDepartment> departments,
                                         Map<String, List<WeComUser>> departmentUserMap,
                                         boolean isUpdate, String operatorId) {
        DataHandleUtils dataHandleUtils = new DataHandleUtils(orgId, departmentUserMap);

        if (isUpdate) {
            // 多次同步 更新
            dataHandleUtils.handleUpdateData(departments, operatorId);
        } else {
            // 首次同步 新增
            dataHandleUtils.handleAddData(departments, operatorId);
        }
    }

    /**
     * 记录同步操作日志
     */
    private void logSyncOperation(LogService logService, String orgId, String operatorId) {
        String detail = Translator.get("log.syncOrganization");
        LogDTO logDTO = new LogDTO(orgId, operatorId, operatorId,
                LogType.SYNC, LogModule.SYSTEM_ORGANIZATION, detail);
        logDTO.setDetail(detail);
        logService.add(logDTO);
    }

    /**
     * 清理相关缓存
     */
    private void clearCaches(String orgId) {
        // 清理部门树缓存
        stringRedisTemplate.delete(DEPT_TREE_CACHE_KEY + orgId);
        // 清理权限缓存
        deleteKeysByPrefixSafely(orgId);
    }

    /**
     * 清理权限缓存
     *
     * @param orgId 组织ID
     */
    public void deleteKeysByPrefixSafely(String orgId) {
        String pattern = String.format(PERMISSION_CACHE_PATTERN, orgId);
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(SCAN_COUNT).build();

        try (Cursor<String> cursor = stringRedisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                stringRedisTemplate.delete(cursor.next());
            }
        }
    }

    /**
     * URL参数转换
     */
    private String urlTransfer(Object... params) {
        Object[] encodedParams = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            if (Objects.isNull(params[i])) {
                encodedParams[i] = "";
                continue;
            }
            String value = StringUtils.stripToEmpty(params[i].toString());
            encodedParams[i] = URLEncoder.encode(value, StandardCharsets.UTF_8);
        }
        return MessageFormat.format(WeComApiPaths.USER_LIST, encodedParams);
    }

    /**
     * 获取部门用户详情
     *
     * @param accessToken   访问令牌
     * @param departmentIds 部门ID列表
     *
     * @return 部门ID与用户列表的映射
     */
    private Map<String, List<WeComUser>> getDepartmentUser(String accessToken, List<Long> departmentIds) {
        Map<String, List<WeComUser>> weComUserMap = new HashMap<>();

        for (Long departmentId : departmentIds) {
            String url = urlTransfer(accessToken, departmentId);
            UserListResponse response = fetchUserList(url);

            if (response.getErrCode() != 0) {
                throw new GenericException("获取用户接口返回结果失败: " + response.getErrMsg());
            }

            weComUserMap.put(departmentId.toString(), response.getUserList());
        }

        return weComUserMap;
    }

    /**
     * 获取用户列表
     */
    private UserListResponse fetchUserList(String url) {
        try {
            String responseStr = HttpRequestUtil.sendGetRequest(url, null);
            return JSON.parseObject(responseStr, UserListResponse.class);
        } catch (Exception e) {
            throw new GenericException("调用接口获取用户列表失败", e);
        }
    }

    /**
     * 获取部门列表
     *
     * @param accessToken 访问令牌
     *
     * @return 部门列表
     */
    private List<WeComDepartment> getDepartmentList(String accessToken) {
        String url = HttpRequestUtil.urlTransfer(WeComApiPaths.DEPARTMENT_LIST, accessToken, null);
        DepartmentListResponse response = fetchDepartmentList(url);

        if (response.getErrCode() != 0) {
            throw new GenericException("获取部门接口返回结果失败: " + response.getErrMsg());
        }

        return response.getDepartment();
    }

    /**
     * 获取部门列表
     */
    private DepartmentListResponse fetchDepartmentList(String url) {
        try {
            String responseStr = HttpRequestUtil.sendGetRequest(url, null);
            return JSON.parseObject(responseStr, DepartmentListResponse.class);
        } catch (Exception e) {
            LogUtils.error(e);
            throw new GenericException("调用接口获取部门列表失败：" + e.getMessage());
        }
    }
}