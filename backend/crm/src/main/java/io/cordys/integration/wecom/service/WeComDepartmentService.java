package io.cordys.integration.wecom.service;

import io.cordys.common.constants.DepartmentConstants;
import io.cordys.common.exception.GenericException;
import io.cordys.common.util.CommonBeanFactory;
import io.cordys.common.util.JSON;
import io.cordys.crm.system.dto.response.SyncOrganizationDTO;
import io.cordys.crm.system.service.OrganizationConfigService;
import io.cordys.crm.system.service.SyncUserService;
import io.cordys.integration.wecom.constant.WeComApiPaths;
import io.cordys.integration.wecom.dto.WeComToken;
import io.cordys.integration.wecom.dto.WeComUser;
import io.cordys.integration.wecom.response.DepartmentListResponse;
import io.cordys.integration.wecom.response.UserListResponse;
import io.cordys.integration.wecom.utils.DepartmentDataHandle;
import io.cordys.integration.wecom.utils.HttpRequestUtil;
import io.cordys.integration.wecom.dto.WeComDepartment;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(rollbackFor = Exception.class)
public class WeComDepartmentService implements SyncUserService {


    /**
     * 企业微信同步组织架构
     *
     * @param operatorId
     * @param orgId
     */
    @Override
    public void syncUser(String operatorId, String orgId) {
        Redisson redisson = CommonBeanFactory.getBean(Redisson.class);
        RLock lock = redisson.getLock("orgId");
        if (lock.tryLock()) {
            try {
                OrganizationConfigService organizationConfigService = CommonBeanFactory.getBean(OrganizationConfigService.class);
                //获取配置信息
                List<SyncOrganizationDTO> synOrganizationConfigs = organizationConfigService.getSynOrganization(orgId);
                if (CollectionUtils.isEmpty(synOrganizationConfigs)) {
                    throw new GenericException("未配置企业信息");
                }
                SyncOrganizationDTO weComConfig = synOrganizationConfigs.stream().filter(config -> StringUtils.equalsIgnoreCase(config.getType(), DepartmentConstants.WECOM.name())).toList().getFirst();
                if (weComConfig == null) {
                    throw new GenericException("未配置企业微信信息");
                }
                //1. 获取assess_token
                String assessToken = getAssessToken(weComConfig.getCorpId(), weComConfig.getAppSecret());
                if (StringUtils.isNotBlank(assessToken)) {
                    //2. 获取部门列表
                    List<WeComDepartment> weComDepartments = getDepartmentList(assessToken);
                    //3. 获取部门成员
                    Map<String, List<WeComUser>> departmentUserMap = getDepartmentUser(assessToken, weComDepartments.stream().map(WeComDepartment::getId).toList());
                    //4. 同步用户
                    DepartmentDataHandle departmentDataHandle = new DepartmentDataHandle(weComDepartments, orgId, departmentUserMap);
                    departmentDataHandle.handleData(operatorId);
                }
            } finally {
                lock.unlock();
            }
        } else {
            throw new GenericException("当前正在执行同步任务！");
        }

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
            String url = urlTransfer(WeComApiPaths.USER_LIST, assessToken, departmentId);
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
        String url = urlTransfer(WeComApiPaths.DEPARTMENT_LIST, assessToken, null);
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


    /**
     * 获取assess_Token
     *
     * @param corpId
     * @param corpSecret
     * @return
     */
    private String getAssessToken(String corpId, String corpSecret) {
        String url = urlTransfer(WeComApiPaths.GET_TOKEN, corpId, corpSecret);
        WeComToken weComToken = new WeComToken();
        try {
            String response = HttpRequestUtil.sendGetRequest(url, null);
            weComToken = JSON.parseObject(response, WeComToken.class);

        } catch (Exception e) {
            throw new GenericException("调用接口获取access_token失败", e);
        }
        if (weComToken.getErrCode() != 0) {
            throw new GenericException("获取access_token接口返回结果失败:" + weComToken.getErrMsg());
        }
        return weComToken.getAccessToken();
    }
}
