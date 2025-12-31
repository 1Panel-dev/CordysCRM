package cn.cordys.crm.integration.sync.service;

import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.dto.LogDTO;
import cn.cordys.common.constants.ThirdConfigTypeConstants;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.LogUtils;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.integration.common.dto.ThirdConfigBaseDTO;
import cn.cordys.crm.integration.common.request.DingTalkThirdConfigRequest;
import cn.cordys.crm.integration.common.request.LarkThirdConfigRequest;
import cn.cordys.crm.integration.common.request.WecomThirdConfigRequest;
import cn.cordys.crm.integration.common.utils.DataHandleUtils;
import cn.cordys.crm.integration.dingtalk.service.DingTalkDepartmentService;
import cn.cordys.crm.integration.lark.service.LarkDepartmentService;
import cn.cordys.crm.integration.sso.service.TokenService;
import cn.cordys.crm.integration.sync.dto.ThirdDepartment;
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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    private LarkDepartmentService larkDepartmentService;

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
        var logService = CommonBeanFactory.getBean(LogService.class);
        LogUtils.info("开始同步组织架构，同步类型：{}", type);

        // 获取三方配置信息
        var thirdConfig = getThirdConfig(orgId, type);
        var syncStatus = integrationConfigService.getSyncStatus(
                orgId, OrganizationConfigConstants.ConfigType.THIRD.name(), type);

        // 获取访问令牌
        var accessToken = getToken(type, thirdConfig);
        if (StringUtils.isBlank(accessToken)) {
            throw new GenericException("获取访问令牌失败");
        }
        LogUtils.info("获取访问令牌成功，同步类型：{}", type);

        // 将字符串类型安全转换为枚举
        var deptType = parseDepartmentType(type);

        List<ThirdDepartment> departments;
        Map<String, List<ThirdUser>> departmentUserMap;

        switch (deptType) {
            case WECOM -> {
                departments = weComDepartmentService.getDepartmentList(accessToken);
                LogUtils.info("企业微信部门数：{}", departments.size());

                var departmentIds = departments.stream()
                        .map(ThirdDepartment::getId)
                        .map(Long::parseLong)
                        .toList();
                departmentUserMap = weComDepartmentService.getDepartmentUser(accessToken, departmentIds);
                LogUtils.info("企业微信部门用户数：{}", departmentUserMap.size());
            }
            case DINGTALK -> {
                var thirdOrgDataDTO = dingTalkDepartmentService.convertToThirdOrgDataDTO(accessToken);
                if (thirdOrgDataDTO == null) {
                    throw new GenericException("钉钉组织数据为空");
                }
                departments = thirdOrgDataDTO.getDepartments();
                departmentUserMap = thirdOrgDataDTO.getUsers();
                LogUtils.info("钉钉部门数：{}，部门用户数：{}", departments.size(), departmentUserMap.size());
            }
            case LARK -> {
                departments = larkDepartmentService.getDepartmentList(accessToken);
                LogUtils.info("飞书部门数：{}", departments.size());

                var departmentIds = departments.stream().map(ThirdDepartment::getId).toList();
                departmentUserMap = larkDepartmentService.getDepartmentUserList(accessToken, departmentIds);
                LogUtils.info("飞书部门用户数：{}", departmentUserMap.size());
            }
            default -> throw new GenericException("不支持的同步类型：" + type);
        }

        if (CollectionUtils.isEmpty(departments)) {
            throw new GenericException("获取部门列表为空");
        }

        syncDepartmentsAndUsers(orgId, departments, departmentUserMap, syncStatus, operatorId, type);

        Objects.requireNonNull(logService, "LogService 未注入");
        LogUtils.info("同步组织架构完成，同步类型：{}", type);
        logSyncOperation(logService, orgId, operatorId);
    }

    private ThirdConfigTypeConstants parseDepartmentType(String type) {
        return Arrays.stream(ThirdConfigTypeConstants.values())
                .filter(e -> e.name().equalsIgnoreCase(type))
                .findFirst()
                .orElseThrow(() -> new GenericException("未知的同步类型：" + type));
    }

    /**
     * 获取第三方访问令牌
     */
    private String getToken(String type, ThirdConfigBaseDTO<?> thirdConfig) {
        Objects.requireNonNull(thirdConfig, "第三方配置信息不能为空");

        var deptType = parseDepartmentType(type);
        var accessToken = switch (deptType) {
            case WECOM -> {
                WecomThirdConfigRequest config = JSON.MAPPER.convertValue(thirdConfig.getConfig(), WecomThirdConfigRequest.class);
                yield tokenService.getAssessToken(config.getCorpId(), config.getAppSecret());
            }
            case DINGTALK -> {
                DingTalkThirdConfigRequest dingTalkConfig = JSON.MAPPER.convertValue(thirdConfig.getConfig(), DingTalkThirdConfigRequest.class);
                yield tokenService.getDingTalkToken(dingTalkConfig.getAgentId(), dingTalkConfig.getAppSecret());
            }
            case LARK -> {
                LarkThirdConfigRequest larkConfig = JSON.MAPPER.convertValue(thirdConfig.getConfig(), LarkThirdConfigRequest.class);
                yield tokenService.getLarkToken(larkConfig.getAgentId(), larkConfig.getAppSecret());
            }
            default -> throw new GenericException("不支持的同步类型：" + type);
        };

        LogUtils.debug("获取访问令牌成功，类型：{}", deptType.name());
        return accessToken;
    }

    /**
     * 获取第三方配置
     */
    private ThirdConfigBaseDTO<?> getThirdConfig(String orgId, String type) {
        List<ThirdConfigBaseDTO<?>> configs = integrationConfigService.getThirdConfig(orgId);
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
            LogUtils.info("开始多次同步更新数据,同步类型：{}, 更新部门用户数量：{}", type, departments.size());
            // 多次同步 更新
            dataHandleUtils.handleUpdateData(departments, operatorId);
        } else {
            // 首次同步 新增
            LogUtils.info("开始首次同步新增数据,同步类型：{}, 新增部门用户数量：{}", type, departments.size());
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