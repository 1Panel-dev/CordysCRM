package cn.cordys.crm.integration.sync.service;

import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.dto.LogDTO;
import cn.cordys.common.constants.DepartmentConstants;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.integration.common.dto.ThirdConfigurationDTO;
import cn.cordys.crm.integration.common.utils.DataHandleUtils;
import cn.cordys.crm.integration.dingtalk.service.DingTalkDepartmentService;
import cn.cordys.crm.integration.sso.service.TokenService;
import cn.cordys.crm.integration.sync.dto.ThirdDepartment;
import cn.cordys.crm.integration.sync.dto.ThirdOrgDataDTO;
import cn.cordys.crm.integration.sync.dto.ThirdUser;
import cn.cordys.crm.integration.wecom.service.WeComDepartmentService;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(rollbackFor = Exception.class)
public class ThirdDepartmentService {

    private static final String LOCK_PREFIX = "orgId_sync_";
    private static final String DEPT_TREE_CACHE_KEY = "dept_tree_cache::";
    private static final String PERMISSION_CACHE_PATTERN = "permission_cache*%s";
    private static final int SCAN_COUNT = 100;

    @Resource
    private TokenService tokenService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private WeComDepartmentService weComDepartmentService;

    @Resource
    private DingTalkDepartmentService dingTalkDepartmentService;

    @Resource
    private IntegrationConfigService integrationConfigService;

    /**
     * 同步组织架构
     *
     * @param operatorId 操作人ID
     * @param orgId      组织ID
     * @param type       同步类型(企业微信，钉钉，飞书)
     */
    public void syncUser(String operatorId, String orgId, String type) {
        Redisson redisson = CommonBeanFactory.getBean(Redisson.class);
        assert redisson != null;
        RLock lock = redisson.getLock(LOCK_PREFIX + orgId);

        // 尝试获取锁，避免并发同步
        if (!lock.tryLock()) {
            throw new GenericException("当前正在执行同步任务！");
        }

        try {
            performSync(operatorId, orgId, type);
            clearCaches(orgId);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 执行同步操作
     */
    private void performSync(String operatorId, String orgId, String type) {
        LogService logService = CommonBeanFactory.getBean(LogService.class);

        // 获取三方配置信息

        ThirdConfigurationDTO thirdConfig = getThirdConfig(orgId, type);
        boolean syncStatus = integrationConfigService.getSyncStatus(orgId,
                OrganizationConfigConstants.ConfigType.THIRD.name(),
                type);
        // 获取访问令牌
        String accessToken = getToken(type, thirdConfig);

        if (StringUtils.isBlank(accessToken)) {
            throw new GenericException("获取访问令牌失败");
        }

        ThirdOrgDataDTO thirdOrgDataDTO = null;
        if (Strings.CI.equals(type, DepartmentConstants.DINGTALK.name())) {
            thirdOrgDataDTO = dingTalkDepartmentService.convertToThirdOrgDataDTO(accessToken);
        }

        List<ThirdDepartment> departments = new ArrayList<>();

        // 获取部门和用户数据
        if (Strings.CI.equals(type, DepartmentConstants.WECOM.name())) {
            departments = weComDepartmentService.getDepartmentList(accessToken);
        } else if (Strings.CI.equals(type, DepartmentConstants.DINGTALK.name())) {
            // 获取钉钉部门列表
            if (thirdOrgDataDTO != null) {
                departments = thirdOrgDataDTO.getDepartments();
            }
        } /*else if (Strings.CI.equals(type, DepartmentConstants.LARK.name())) {
            // 获取飞书部门列表
        }*/ else {
            throw new GenericException("不支持的同步类型");
        }

        if (CollectionUtils.isEmpty(departments)) {
            throw new GenericException("获取部门列表为空");
        }

        List<Long> departmentIds = departments.stream().map(ThirdDepartment::getId).toList();
        Map<String, List<ThirdUser>> departmentUserMap = new HashMap<>();

        if (Strings.CI.equals(type, DepartmentConstants.WECOM.name())) {
            // 获取企业微信部门用户
            departmentUserMap = weComDepartmentService.getDepartmentUser(accessToken, departmentIds);
        } else if (Strings.CI.equals(type, DepartmentConstants.DINGTALK.name())) {
            // 获取钉钉部门用户
            if (thirdOrgDataDTO != null) {
                departmentUserMap = thirdOrgDataDTO.getUsers();
            }
        } /*else if (Strings.CI.equals(type, DepartmentConstants.LARK.name())) {
            // 获取飞书部门用户
        }*/ else {
            throw new GenericException("不支持的同步类型");
        }

        // 处理同步数据
        syncDepartmentsAndUsers(orgId, departments, departmentUserMap, syncStatus, operatorId, type);

        // 记录日志
        assert logService != null;
        logSyncOperation(logService, orgId, operatorId);
    }

    /**
     * 获取第三方访问令牌
     */
    private String getToken(String type, ThirdConfigurationDTO thirdConfig) {
        String accessToken;
        //根据类型获取不同平台的访问令牌
        if (Strings.CI.equals(type, DepartmentConstants.WECOM.name())) {
            // 获取企业微信访问令牌
            accessToken = tokenService.getAssessToken(thirdConfig.getCorpId(), thirdConfig.getAppSecret());
        } else if (Strings.CI.equals(type, DepartmentConstants.DINGTALK.name())) {
            // 获取钉钉访问令牌
            accessToken = tokenService.getDingTalkToken(thirdConfig.getAgentId(), thirdConfig.getAppSecret());
        } else if (Strings.CI.equals(type, DepartmentConstants.LARK.name())) {
            // 获取飞书访问令牌
            accessToken = tokenService.getLarkToken(thirdConfig.getAgentId(), thirdConfig.getAppSecret());
        } else {
            throw new GenericException("不支持的同步类型");
        }
        return accessToken;
    }

    /**
     * 获取第三方配置
     */
    private ThirdConfigurationDTO getThirdConfig(String orgId, String type) {
        List<ThirdConfigurationDTO> configs = integrationConfigService.getThirdConfig(orgId);
        if (CollectionUtils.isEmpty(configs)) {
            throw new GenericException("未配置企业信息");
        }

        return configs.stream()
                .filter(config -> Strings.CI.equals(config.getType(), type))
                .findFirst()
                .orElseThrow(() -> new GenericException("未配置企业微信信息"));
    }

    /**
     * 同步部门和用户数据
     */
    private void syncDepartmentsAndUsers(String orgId, List<ThirdDepartment> departments,
                                         Map<String, List<ThirdUser>> departmentUserMap,
                                         boolean isUpdate, String operatorId, String type) {
        DataHandleUtils dataHandleUtils = new DataHandleUtils(orgId, departmentUserMap, type);

        if (isUpdate) {
            // 多次同步 更新
            dataHandleUtils.handleUpdateData(departments, operatorId);
        } else {
            // 首次同步 新增
            dataHandleUtils.handleAddData(departments, operatorId, orgId, type);
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

}