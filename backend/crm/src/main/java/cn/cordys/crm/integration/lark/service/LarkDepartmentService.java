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

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LarkDepartmentService {

    private static final int DEPARTMENT_USER_PAGE_SIZE = 50;
    private static final int maxLoop = 1000;
    @Resource
    private QrCodeClient qrCodeClient;

    public List<LarkDepartment> getAllSubDepartments(String tenantAccessToken, String departmentId) {
        List<LarkDepartment> allDepartments = new ArrayList<>();
        String pageToken = null;
        boolean hasMore = true;
        int loop = 0;

        while (hasMore && loop++ < maxLoop) {
            StringBuilder urlBuilder = new StringBuilder(LarkApiPaths.LARK_CHILDREN_DEPARTMENT_URL.replace("{0}", departmentId));
            urlBuilder.append("?fetch_child=true");
            if (StringUtils.isNotBlank(pageToken)) {
                urlBuilder.append("&page_token=").append(pageToken);
            }

            LarkDepartmentResponse larkResponse = safeRequest(urlBuilder.toString(), tenantAccessToken, LarkDepartmentResponse.class);
            hasMore = false;
            if (larkResponse != null && larkResponse.getCode() == 0) {
                LarkDepartmentData data = larkResponse.getData();
                if (data != null) {
                    allDepartments.addAll(Optional.ofNullable(data.getItems()).orElse(Collections.emptyList()));
                    hasMore = data.isHasMore();
                    pageToken = data.getPageToken();
                }
            } else if (larkResponse != null) {
                log.error("获取飞书子部门信息失败: {}:{}", larkResponse.getCode(), larkResponse.getMsg());
            }
        }

        return allDepartments;
    }

    public List<LarkUser> getDepartmentUsers(String tenantAccessToken, String departmentId) {
        List<LarkUser> allUsers = new ArrayList<>();
        String pageToken = null;
        boolean hasMore = true;
        int loop = 0;

        while (hasMore && loop++ < maxLoop) {
            StringBuilder urlBuilder = new StringBuilder(LarkApiPaths.LARK_DEPARTMENT_USERS_URL);
            urlBuilder.append("?department_id=").append(departmentId)
                    .append("&page_size=").append(DEPARTMENT_USER_PAGE_SIZE);
            if (StringUtils.isNotBlank(pageToken)) {
                urlBuilder.append("&page_token=").append(pageToken);
            }

            LarkUserResponse larkUserResponse = safeRequest(urlBuilder.toString(), tenantAccessToken, LarkUserResponse.class);
            hasMore = false;
            if (larkUserResponse != null && larkUserResponse.getCode() == 0) {
                LarkUserResponse.LarkUserData data = larkUserResponse.getData();
                if (data != null) {
                    allUsers.addAll(Optional.ofNullable(data.getItems()).orElse(Collections.emptyList()));
                    hasMore = data.isHasMore();
                    pageToken = data.getPageToken();
                }
            } else if (larkUserResponse != null) {
                log.error("获取飞书部门用户失败: {}:{}", larkUserResponse.getCode(), larkUserResponse.getMsg());
            }
        }

        return allUsers;
    }

    public List<ThirdDepartment> getDepartmentList(String tenantAccessToken) {
        List<ThirdDepartment> thirdDepartments = getAllSubDepartments(tenantAccessToken, "0").stream()
                .map(this::toThirdDepartment)
                .collect(Collectors.toList());
        thirdDepartments.add(buildRootDepartment(getTenantInfo(tenantAccessToken)));
        return thirdDepartments;
    }

    public Map<String, List<ThirdUser>> getDepartmentUserList(String tenantAccessToken, List<String> departmentIds) {
        return departmentIds.stream().collect(Collectors.toMap(Function.identity(), departmentId ->
                getDepartmentUsers(tenantAccessToken, departmentId).stream()
                        .filter(user -> isPrimaryDepartmentUser(user, departmentId))
                        .map(this::toThirdUser)
                        .collect(Collectors.toList())
        ));
    }

    public LarkTenant getTenantInfo(String tenantAccessToken) {
        try {
            LarkTenantResponse larkTenantResponse = safeRequest(LarkApiPaths.LARK_TENANT_INFO_URL, tenantAccessToken, LarkTenantResponse.class);
            if (larkTenantResponse != null && larkTenantResponse.getCode() == 0) {
                return Optional.ofNullable(larkTenantResponse.getData()).map(LarkTenantResponse.LarkTenantData::getTenant).orElse(null);
            }
            if (larkTenantResponse != null) {
                log.error("获取企业信息失败: {}:{}", larkTenantResponse.getCode(), larkTenantResponse.getMsg());
            }
        } catch (Exception e) {
            log.error("获取飞书企业信息失败", e);
        }
        return null;
    }

    private <T> T safeRequest(String url, String tenantAccessToken, Class<T> responseClass) {
        String response = qrCodeClient.exchange(
                url,
                "Bearer " + tenantAccessToken,
                "Authorization",
                MediaType.APPLICATION_JSON,
                MediaType.APPLICATION_JSON
        );
        return JSON.parseObject(response, responseClass);
    }

    private ThirdDepartment toThirdDepartment(LarkDepartment larkDepartment) {
        ThirdDepartment thirdDepartment = new ThirdDepartment();
        thirdDepartment.setId(larkDepartment.getOpenDepartmentId());
        thirdDepartment.setName(larkDepartment.getName());
        thirdDepartment.setParentId(larkDepartment.getParentDepartmentId());
        thirdDepartment.setOrder(Long.valueOf(larkDepartment.getOrder()));
        thirdDepartment.setIsRoot("0".equals(larkDepartment.getOpenDepartmentId()));
        return thirdDepartment;
    }

    private ThirdDepartment buildRootDepartment(LarkTenant tenant) {
        ThirdDepartment rootDept = new ThirdDepartment();
        rootDept.setId("0");
        rootDept.setName(Optional.ofNullable(tenant).map(LarkTenant::getName).orElse("公司名称"));
        rootDept.setParentId("NONE");
        rootDept.setIsRoot(true);
        rootDept.setOrder(0L);
        return rootDept;
    }

    private ThirdUser toThirdUser(LarkUser larkUser) {
        return ThirdUser.builder()
                .userId(larkUser.getOpenId())
                .name(larkUser.getName())
                .email(larkUser.getEmail())
                .mobile(formatMobile(larkUser.getMobile()))
                .position(larkUser.getWorkStation())
                .gender(larkUser.getGender())
                .avatar(Optional.ofNullable(larkUser.getAvatar())
                        .map(LarkUser.LarkUserAvatar::getAvatar240)
                        .filter(StringUtils::isNotBlank)
                        .orElse(null))
                .isLeaderInDept(Strings.CI.equals(larkUser.getLeaderUserId(), larkUser.getUserId()))
                .build();
    }

    private boolean isPrimaryDepartmentUser(LarkUser user, String departmentId) {
        return Optional.ofNullable(user.getOrders()).orElse(Collections.emptyList()).stream()
                .anyMatch(order -> order.getIsPrimaryDept() && Strings.CS.equals(order.getDepartmentId(), departmentId));
    }

    private String formatMobile(String mobile) {
        if (StringUtils.isBlank(mobile)) {
            return null;
        }
        return mobile.length() > 11 ? mobile.substring(mobile.length() - 11) : mobile;
    }
}
