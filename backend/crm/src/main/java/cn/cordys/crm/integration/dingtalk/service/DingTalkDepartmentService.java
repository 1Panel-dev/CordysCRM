package cn.cordys.crm.integration.dingtalk.service;

import cn.cordys.common.util.JSON;
import cn.cordys.common.util.LogUtils;
import cn.cordys.crm.integration.common.utils.HttpRequestUtil;
import cn.cordys.crm.integration.dingtalk.constant.DingTalkApiPaths;
import cn.cordys.crm.integration.dingtalk.dto.DingTalkDepartment;
import cn.cordys.crm.integration.dingtalk.dto.DingTalkUser;
import cn.cordys.crm.integration.dingtalk.response.DepartmentDetailsResponse;
import cn.cordys.crm.integration.dingtalk.response.DingTalkOrgDataResponse;
import cn.cordys.crm.integration.dingtalk.response.SubDeptIdListResponse;
import cn.cordys.crm.integration.dingtalk.response.UserListResponse;
import cn.cordys.crm.integration.sync.dto.ThirdDepartment;
import cn.cordys.crm.integration.sync.dto.ThirdOrgDataDTO;
import cn.cordys.crm.integration.sync.dto.ThirdUser;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
public class DingTalkDepartmentService {


    public ThirdOrgDataDTO convertToThirdOrgDataDTO(String accessToken) {
        DingTalkOrgDataResponse response = getOrganizationAndUsers(accessToken);
        ThirdOrgDataDTO thirdOrgDataDTO = new ThirdOrgDataDTO();
        thirdOrgDataDTO.setDepartments(Optional.ofNullable(response.getDepartments())
                .orElse(Collections.emptyList())
                .stream()
                .map(this::convertToThirdDepartment)
                .toList());
        Map<String, List<ThirdUser>> thirdUsersByDept = Optional.ofNullable(response.getUsers())
                .orElse(Collections.emptyMap())
                .entrySet()
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        entry -> entry.getKey().toString(),
                        entry -> entry.getValue().stream()
                                .map(this::convertToThirdUser)
                                .collect(java.util.stream.Collectors.toList())
                ));
        thirdOrgDataDTO.setUsers(thirdUsersByDept);


        return thirdOrgDataDTO;
    }

    private ThirdUser convertToThirdUser(DingTalkUser dingTalkUser) {
        return ThirdUser.builder()
                .userId(dingTalkUser.getUserId())
                .name(dingTalkUser.getName())
                .mobile(dingTalkUser.getMobile())
                .email(dingTalkUser.getEmail())
                .isLeaderInDept(dingTalkUser.getLeader())
                .avatar(dingTalkUser.getAvatar())
                .position(dingTalkUser.getTitle())
                .gender(1)
                .build();
    }


    private ThirdDepartment convertToThirdDepartment(DingTalkDepartment dingTalkDepartment) {
        var thirdDepartment = new ThirdDepartment();

        thirdDepartment.setId(String.valueOf(dingTalkDepartment.getDeptId()));
        thirdDepartment.setName(dingTalkDepartment.getName());

        // 判断根部门
        var isRoot = dingTalkDepartment.getDeptId() == 1;
        thirdDepartment.setIsRoot(isRoot);

        // 根部门和普通部门的 parentId 不同处理
        thirdDepartment.setParentId(isRoot ? "NONE" : String.valueOf(dingTalkDepartment.getParentId()));

        // 排序
        thirdDepartment.setOrder(dingTalkDepartment.getOrder());

        return thirdDepartment;
    }

    /**
     * 获取所有钉钉组织架构和用户
     *
     * @param accessToken 访问令牌
     * @return 组织架构和用户数据响应
     */
    public DingTalkOrgDataResponse getOrganizationAndUsers(String accessToken) {
        List<Long> allDepartmentIds = getAllSubDepartmentIds(accessToken, 1L); // 从根部门(ID=1)开始
        DingTalkOrgDataResponse response = new DingTalkOrgDataResponse();

        try {
            List<DingTalkDepartment> departments = new ArrayList<>();
            Map<Long, List<DingTalkUser>> usersByDept = new HashMap<>();

            for (Long deptId : allDepartmentIds) {
                // 获取部门详情
                getDepartmentDetail(accessToken, deptId).ifPresent(departments::add);
                // 获取部门用户
                List<DingTalkUser> users = getUsersByDepartment(accessToken, deptId);
                List<DingTalkUser> filteredUsers = new ArrayList<>();
                for (DingTalkUser user : users) {
                    //用户dept_id_list 获取第一个部门ID进行匹配(主部门)
                    if (user.getDeptIdList() != null && !user.getDeptIdList().isEmpty()
                            && user.getDeptIdList().getFirst().equals(deptId)) {
                        filteredUsers.add(user);
                    }
                }
                usersByDept.put(deptId, filteredUsers);
            }

            response.setDepartments(departments);
            response.setUsers(usersByDept);
        } catch (Exception e) {
            LogUtils.error(e);
        }
        return response;
    }


    /**
     * 递归获取所有子部门ID
     *
     * @param accessToken 访问令牌
     * @param deptId      部门ID
     * @return 部门ID列表
     */
    private List<Long> getAllSubDepartmentIds(String accessToken, Long deptId) {
        List<Long> departmentIds = new ArrayList<>();
        departmentIds.add(deptId);

        String url = HttpRequestUtil.urlTransfer(DingTalkApiPaths.DING_DEPARTMENT_IDS, accessToken);
        String requestBody = JSON.toJSONString(Collections.singletonMap("dept_id", deptId));

        try {
            String responseBody = HttpRequestUtil.sendPostRequest(url, requestBody, null);
            SubDeptIdListResponse response = JSON.parseObject(responseBody, SubDeptIdListResponse.class);

            if (response != null && response.getErrCode() == 0 && response.getResult() != null) {
                for (Long subDeptId : response.getResult().getDeptIdList()) {
                    departmentIds.addAll(getAllSubDepartmentIds(accessToken, subDeptId));
                }
            }
        } catch (IOException | InterruptedException e) {
            LogUtils.error("获取子部门ID失败", e);
            // 适当处理异常，例如记录日志
            Thread.currentThread().interrupt();
        }
        return departmentIds;
    }


    /**
     * 获取单个部门详情
     *
     * @param accessToken 访问令牌
     * @param deptId      部门ID
     * @return 部门详情Optional
     */
    private Optional<DingTalkDepartment> getDepartmentDetail(String accessToken, Long deptId) {
        String url = HttpRequestUtil.urlTransfer(DingTalkApiPaths.DING_DEPARTMENT_DETAIL, accessToken);
        String requestBody = JSON.toJSONString(Map.of("dept_id", deptId, "language", "zh_CN"));
        try {
            String responseBody = HttpRequestUtil.sendPostRequest(url, requestBody, null);
            DepartmentDetailsResponse response = JSON.parseObject(responseBody, DepartmentDetailsResponse.class);

            if (response != null && response.getErrCode() == 0 && response.getResult() != null) {
                return Optional.of(response.getResult());
            }
        } catch (IOException | InterruptedException e) {
            LogUtils.error("获取部门详情失败", e);
            // 适当处理异常，例如记录日志
            Thread.currentThread().interrupt();
        }
        return Optional.empty();
    }


    /**
     * 获取单个部门下的用户列表（处理分页）
     *
     * @param accessToken 访问令牌
     * @param deptId      部门ID
     * @return 用户列表
     */
    private List<DingTalkUser> getUsersByDepartment(String accessToken, Long deptId) {
        List<DingTalkUser> users = new ArrayList<>();
        long cursor = 0L;
        boolean hasMore;

        String url = HttpRequestUtil.urlTransfer(DingTalkApiPaths.DING_DEPARTMENT_USER_DETAIL_LIST, accessToken);

        do {
            Map<String, Object> requestBodyMap = Map.of(
                    "dept_id", deptId,
                    "cursor", cursor,
                    "size", 100 // 最大100
            );
            String requestBody = JSON.toJSONString(requestBodyMap);

            try {
                String responseBody = HttpRequestUtil.sendPostRequest(url, requestBody, null);
                UserListResponse response = JSON.parseObject(responseBody, UserListResponse.class);

                if (response != null && response.getErrcode() == 0 && response.getResult() != null) {
                    users.addAll(response.getResult().getList());
                    hasMore = response.getResult().getHasMore();
                    if (hasMore && response.getResult().getNextCursor() != null) {
                        cursor = response.getResult().getNextCursor();
                    }
                } else {
                    hasMore = false;
                }
            } catch (IOException | InterruptedException e) {
                LogUtils.error("获取部门用户失败", e);
                Thread.currentThread().interrupt();
                hasMore = false;
            }
        } while (hasMore);

        return users;
    }


}