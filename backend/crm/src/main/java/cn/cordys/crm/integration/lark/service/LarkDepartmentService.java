package cn.cordys.crm.integration.lark.service;

import cn.cordys.common.util.JSON;
import cn.cordys.crm.integration.common.client.QrCodeClient;
import cn.cordys.crm.integration.lark.constant.LarkApiPaths;
import cn.cordys.crm.integration.lark.dto.LarkDepartment;
import cn.cordys.crm.integration.lark.dto.LarkDepartmentData;
import cn.cordys.crm.integration.lark.dto.LarkTenant;
import cn.cordys.crm.integration.lark.dto.LarkUser;
import cn.cordys.crm.integration.lark.response.LarkDepartmentResponse;
import cn.cordys.crm.integration.lark.response.LarkTenantResponse;
import cn.cordys.crm.integration.lark.response.LarkUserResponse;
import cn.cordys.crm.integration.sync.dto.ThirdDepartment;
import cn.cordys.crm.integration.sync.dto.ThirdUser;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LarkDepartmentService {

    @Resource
    private QrCodeClient qrCodeClient;

    /**
     * 递归获取所有子部门信息
     *
     * @param tenantAccessToken 租户访问令牌
     * @param departmentId      根部门ID
     * @return 子部门列表
     */
    public List<LarkDepartment> getAllSubDepartments(String tenantAccessToken, String departmentId) {
        List<LarkDepartment> allDepartments = new ArrayList<>();
        String pageToken = null;
        boolean hasMore;

        do {
            String url = LarkApiPaths.LARK_CHILDREN_DEPARTMENT_URL.replace("{0}", departmentId) + "?fetch_child=true";
            if (pageToken != null) {
                url += "&page_token=" + pageToken;
            }

            String response = qrCodeClient.exchange(
                    url,
                    "Bearer " + tenantAccessToken,
                    "Authorization",
                    MediaType.APPLICATION_JSON,
                    MediaType.APPLICATION_JSON
            );

            LarkDepartmentResponse larkResponse = JSON.parseObject(response, LarkDepartmentResponse.class);
            if (larkResponse.getCode() == 0) {
                LarkDepartmentData data = larkResponse.getData();
                if (data != null && data.getItems() != null) {
                    allDepartments.addAll(data.getItems());
                }
                hasMore = data != null && data.isHasMore();
                pageToken = data != null ? data.getPageToken() : null;
            } else {
                log.error("获取飞书子部门信息失败: {}", larkResponse.getCode() + ":" + larkResponse.getMsg());
                hasMore = false;
            }
        } while (hasMore);

        return allDepartments;
    }

    /**
     * 获取部门直属用户
     *
     * @param tenantAccessToken 租户访问令牌
     * @param departmentId      部门ID
     * @return 部门直属用户列表
     */
    public List<LarkUser> getDepartmentUsers(String tenantAccessToken, String departmentId) {
        List<LarkUser> allUsers = new ArrayList<>();
        String pageToken = null;
        boolean hasMore;
        int pageSize = 50; // 飞书API默认和最大分页大小

        do {
            StringBuilder urlBuilder = new StringBuilder(LarkApiPaths.LARK_DEPARTMENT_USERS_URL);
            urlBuilder.append("?department_id=").append(departmentId);
            urlBuilder.append("&page_size=").append(pageSize);
            if (pageToken != null) {
                urlBuilder.append("&page_token=").append(pageToken);
            }

            String response = qrCodeClient.exchange(
                    urlBuilder.toString(),
                    "Bearer " + tenantAccessToken,
                    "Authorization",
                    MediaType.APPLICATION_JSON,
                    MediaType.APPLICATION_JSON
            );

            LarkUserResponse larkUserResponse = JSON.parseObject(response, LarkUserResponse.class);
            if (larkUserResponse.getCode() == 0) {
                LarkUserResponse.LarkUserData data = larkUserResponse.getData();
                if (data != null && data.getItems() != null) {
                    allUsers.addAll(data.getItems());
                }
                hasMore = data != null && data.isHasMore();
                pageToken = data != null ? data.getPageToken() : null;
            } else {
                log.error("获取飞书部门用户失败: {}", larkUserResponse.getCode() + ":" + larkUserResponse.getMsg());
                hasMore = false;
            }
        } while (hasMore);

        return allUsers;
    }


    /**
     * 将飞书部门转化为CRM部门
     *
     * @param tenantAccessToken 租户访问令牌
     * @return CRM部门列表
     */
    public List<ThirdDepartment> getDepartmentList(String tenantAccessToken) {
        List<LarkDepartment> larkDepartments = getAllSubDepartments(tenantAccessToken, "0");
        List<ThirdDepartment> thirdDepartments = new ArrayList<>();
        for (LarkDepartment larkDept : larkDepartments) {
            ThirdDepartment thirdDept = new ThirdDepartment();
            thirdDept.setId(larkDept.getOpenDepartmentId());
            thirdDept.setName(larkDept.getName());
            thirdDept.setParentId(larkDept.getParentDepartmentId());
            thirdDept.setOrder(Long.valueOf(larkDept.getOrder()));
            thirdDept.setIsRoot("0".equals(larkDept.getOpenDepartmentId()));
            thirdDepartments.add(thirdDept);
        }
        LarkTenant tenant = getTenantInfo(tenantAccessToken);
        if (tenant != null) {
            ThirdDepartment rootDept = new ThirdDepartment();
            rootDept.setId("0");
            rootDept.setName(tenant.getName());
            rootDept.setParentId("NONE");
            rootDept.setIsRoot(true);
            rootDept.setOrder(0L);
            thirdDepartments.add(rootDept);
        } else {
            ThirdDepartment rootDept = new ThirdDepartment();
            rootDept.setId("0");
            rootDept.setName("公司名称");
            rootDept.setParentId("NONE");
            rootDept.setIsRoot(true);
            rootDept.setOrder(0L);
            thirdDepartments.add(rootDept);
        }
        return thirdDepartments;
    }

    /**
     * 获取部门直属用户的CRM用户列表
     *
     * @param tenantAccessToken 租户访问令牌
     * @param departmentIds     部门ID列表
     * @return 部门ID与用户列表的映射
     */
    public Map<String, List<ThirdUser>> getDepartmentUserList(String tenantAccessToken, List<String> departmentIds) {
        Map<String, List<ThirdUser>> thirdUserMap = new HashMap<>();

        departmentIds.forEach(departmentId -> {
            List<LarkUser> larkUsers = getDepartmentUsers(tenantAccessToken, departmentId);
            thirdUserMap.put(departmentId, larkUsers.stream().filter(larkUser ->
                            larkUser.getOrders().stream().anyMatch(order -> order.getIsPrimaryDept() && order.getDepartmentId().equals(departmentId)))
                    .map(larkUser -> ThirdUser.builder()
                            .userId(larkUser.getOpenId())
                            .name(larkUser.getName())
                            .email(larkUser.getEmail())
                            .mobile(larkUser.getMobile() != null && larkUser.getMobile().length() > 11 ? larkUser.getMobile().substring(larkUser.getMobile().length() - 11) : larkUser.getMobile())
                            .position(larkUser.getWorkStation())
                            .gender(larkUser.getGender() == null ? null : larkUser.getGender())
                            .avatar((larkUser.getAvatar() == null || StringUtils.isBlank(larkUser.getAvatar().getAvatar240())) ? null : larkUser.getAvatar().getAvatar240())
                            .isLeaderInDept(Strings.CI.equals(larkUser.getLeaderUserId(), larkUser.getUserId()))
                            .build())
                    .collect(Collectors.toList()));
        });
        return thirdUserMap;
    }

    /**
     * 获取企业信息
     *
     * @param tenantAccessToken 租户访问令牌
     * @return 企业信息
     */
    public LarkTenant getTenantInfo(String tenantAccessToken) {
        try {
            String response = qrCodeClient.exchange(
                    LarkApiPaths.LARK_TENANT_INFO_URL,
                    "Bearer " + tenantAccessToken,
                    "Authorization",
                    MediaType.APPLICATION_JSON,
                    MediaType.APPLICATION_JSON
            );

            LarkTenantResponse larkTenantResponse = JSON.parseObject(response, LarkTenantResponse.class);
            if (larkTenantResponse.getCode() == 0) {
                LarkTenantResponse.LarkTenantData data = larkTenantResponse.getData();
                if (data != null) {
                    return data.getTenant();
                }
                return null;
            } else {
                log.error("获取企业信息失败: {}", larkTenantResponse.getCode() + ":" + larkTenantResponse.getMsg());
                return null;
            }
        } catch (Exception e) {
            log.error("获取飞书企业信息失败", e);
            // 根据您的要求，捕获错误并可以返回 null 或抛出自定义异常
            // 这里我们返回 null
            return null;
        }
    }
}