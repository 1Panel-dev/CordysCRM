package io.cordys.crm.integration.wecom.service;


import io.cordys.aspectj.constants.LogModule;
import io.cordys.aspectj.constants.LogType;
import io.cordys.aspectj.dto.LogDTO;
import io.cordys.common.constants.DepartmentConstants;
import io.cordys.common.exception.GenericException;
import io.cordys.common.util.CommonBeanFactory;
import io.cordys.common.util.JSON;
import io.cordys.common.util.Translator;
import io.cordys.crm.system.constants.OrganizationConfigConstants;
import io.cordys.crm.system.service.LogService;
import io.cordys.crm.integration.auth.dto.ThirdConfigurationDTO;
import io.cordys.crm.system.service.IntegrationConfigService;
import io.cordys.crm.integration.sso.service.TokenService;
import io.cordys.crm.integration.wecom.constant.WeComApiPaths;
import io.cordys.crm.integration.wecom.dto.WeComDepartment;
import io.cordys.crm.integration.wecom.dto.WeComUser;
import io.cordys.crm.integration.wecom.response.DepartmentListResponse;
import io.cordys.crm.integration.wecom.response.UserListResponse;
import io.cordys.crm.integration.wecom.utils.DataHandleUtils;
import io.cordys.crm.integration.wecom.utils.HttpRequestUtil;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
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

    @Resource
    private TokenService tokenService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 企业微信同步组织架构
     *
     * @param operatorId
     * @param orgId
     */
    public void syncUser(String operatorId, String orgId) {
        Redisson redisson = CommonBeanFactory.getBean(Redisson.class);
        RLock lock = redisson.getLock("orgId_sync_" + orgId);
        if (lock.tryLock()) {
            try {
                IntegrationConfigService organizationConfigService = CommonBeanFactory.getBean(IntegrationConfigService.class);
                LogService logService = CommonBeanFactory.getBean(LogService.class);
                //获取配置信息
                List<ThirdConfigurationDTO> synOrganizationConfigs = organizationConfigService.getThirdConfig(orgId);
                if (CollectionUtils.isEmpty(synOrganizationConfigs)) {
                    throw new GenericException("未配置企业信息");
                }
                ThirdConfigurationDTO weComConfig = synOrganizationConfigs.stream().filter(config -> StringUtils.equalsIgnoreCase(config.getType(), DepartmentConstants.WECOM.name())).toList().getFirst();
                if (weComConfig == null) {
                    throw new GenericException("未配置企业微信信息");
                }
                boolean syncStatus = organizationConfigService.getSyncStatus(orgId, OrganizationConfigConstants.ConfigType.THIRD.name(), DepartmentConstants.WECOM.name());
                //1. 获取assess_token
                String assessToken = tokenService.getAssessToken(weComConfig.getCorpId(), weComConfig.getAppSecret());
                if (StringUtils.isNotBlank(assessToken)) {
                    //2. 获取部门列表
                    List<WeComDepartment> weComDepartments = getDepartmentList(assessToken);
                    //3. 获取部门成员
                    Map<String, List<WeComUser>> departmentUserMap = getDepartmentUser(assessToken, weComDepartments.stream().map(WeComDepartment::getId).toList());
                    //4. 同步用户
                    DataHandleUtils dataHandleUtils = new DataHandleUtils(orgId, departmentUserMap);
                    if (syncStatus) {
                        //多次同步 更新
                        dataHandleUtils.handleUpdateData(weComDepartments, operatorId);
                    } else {
                        //首次同步 新增
                        dataHandleUtils.handleAddData(weComDepartments, operatorId);
                    }

                    LogDTO logDTO = new LogDTO(orgId, operatorId, operatorId, LogType.SYNC, LogModule.SYSTEM_ORGANIZATION, Translator.get("log.syncOrganization"));
                    String detail = Translator.get("log.syncOrganization");
                    logDTO.setDetail(detail);
                    logService.add(logDTO);
                }
                stringRedisTemplate.delete("dept_tree_cache::" + orgId);
                deleteKeysByPrefixSafely(orgId);
            } finally {
                lock.unlock();
            }
        } else {
            throw new GenericException("当前正在执行同步任务！");
        }

    }


    /**
     * 清理权限缓存
     *
     * @param orgId
     */
    public void deleteKeysByPrefixSafely(String orgId) {
        String pattern = "permission_cache" + "*" + orgId;
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(100).build();
        try (Cursor<String> cursor = stringRedisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                stringRedisTemplate.delete(key);
            }
        }
    }

    String urlTransfer(Object... params) {
        Object[] vars = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            if (Objects.isNull(params[i])) {
                vars[i] = "";
                continue;
            }
            String var = StringUtils.stripToEmpty(params[i].toString());
            vars[i] = URLEncoder.encode(var, StandardCharsets.UTF_8);
        }
        return MessageFormat.format(WeComApiPaths.USER_LIST, vars);
    }


    /**
     * 获取部门用户详情
     *
     * @param assessToken
     * @param departmentIds
     * @return
     */
    private Map<String, List<WeComUser>> getDepartmentUser(String assessToken, List<Long> departmentIds) {
        Map<String, List<WeComUser>> weComUserMap = new HashMap<>();
        departmentIds.forEach(departmentId -> {
            UserListResponse userListResponse = new UserListResponse();
            String url = urlTransfer(assessToken, departmentId);
            try {
                String response = HttpRequestUtil.sendGetRequest(url, null);
                userListResponse = JSON.parseObject(response, UserListResponse.class);
            } catch (Exception e) {
                throw new GenericException("调用接口获取用户列表失败", e);
            }
            if (userListResponse.getErrCode() != 0) {
                throw new GenericException("获取用户接口返回结果失败:" + userListResponse.getErrMsg());
            }
            weComUserMap.put(departmentId.toString(), userListResponse.getUserList());
        });
        return weComUserMap;
    }

    /**
     * 获取部门id列表
     *
     * @param assessToken
     * @return
     */
    private List<WeComDepartment> getDepartmentList(String assessToken) {
        String url = HttpRequestUtil.urlTransfer(WeComApiPaths.DEPARTMENT_LIST, assessToken, null);
        DepartmentListResponse departmentListResponse = new DepartmentListResponse();
        try {
            String response = HttpRequestUtil.sendGetRequest(url, null);
            departmentListResponse = JSON.parseObject(response, DepartmentListResponse.class);
        } catch (Exception e) {
            throw new GenericException("调用接口获取部门列表失败", e);
        }

        if (departmentListResponse.getErrCode() != 0) {
            throw new GenericException("获取部门接口返回结果失败:" + departmentListResponse.getErrMsg());
        }

        return departmentListResponse.getDepartment();
    }


}
