package cn.cordys.crm.integration.dingtalk.service;

import cn.cordys.common.util.JSON;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class DingTalkDepartmentService {

    // 钉钉API限流配置
    private static final int DINGTALK_API_RATE_LIMIT = 20; // 每秒最多20次请求
    private static final long MIN_REQUEST_INTERVAL = 1000L / DINGTALK_API_RATE_LIMIT; // 最小请求间隔(毫秒)

    // 请求计数器（用于监控）
    private final AtomicInteger apiRequestCount = new AtomicInteger(0);


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

        if (thirdOrgDataDTO.getDepartments().isEmpty()) {
            log.info("钉钉组织数据为空,请重新同步");
        }
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

            log.info("开始处理{}个部门的数据同步", allDepartmentIds.size());

            // 批量处理部门信息，添加限流控制
            for (int i = 0; i < allDepartmentIds.size(); i++) {
                Long deptId = allDepartmentIds.get(i);

                // 每批次请求后添加延迟，避免触发限流
                if (i > 0) {
                    try {
                        // 根据请求频率动态调整延迟
                        long delay = calculateDelay(i);
                        if (delay > 0) {
                            log.info("添加延迟：{}毫秒", delay);
                            TimeUnit.MILLISECONDS.sleep(delay);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

                log.info("正在处理部门ID: {} ({}/{})", deptId, i + 1, allDepartmentIds.size());

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

            log.info("数据同步完成，共处理{}个部门，API请求数: {}",
                    allDepartmentIds.size(), apiRequestCount.get());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return response;
    }


    /**
     * 使用队列方式获取所有子部门ID（避免递归，更好的限流控制）
     *
     * @param accessToken 访问令牌
     * @param rootDeptId  根部门ID
     * @return 部门ID列表
     */
    private List<Long> getAllSubDepartmentIds(String accessToken, Long rootDeptId) {
        List<Long> departmentIds = new ArrayList<>();
        Queue<Long> deptQueue = new LinkedList<>();
        deptQueue.offer(rootDeptId);

        while (!deptQueue.isEmpty()) {
            Long currentDeptId = deptQueue.poll();
            departmentIds.add(currentDeptId);

            String url = HttpRequestUtil.urlTransfer(DingTalkApiPaths.DING_DEPARTMENT_IDS, accessToken);
            String requestBody = JSON.toJSONString(Collections.singletonMap("dept_id", currentDeptId));

            try {
                // 限流控制
                applyRateLimit();

                String responseBody = HttpRequestUtil.sendPostRequest(url, requestBody, null);
                SubDeptIdListResponse response = JSON.parseObject(responseBody, SubDeptIdListResponse.class);

                if (response != null && response.getErrCode() == 0 && response.getResult() != null) {
                    List<Long> subDeptIds = response.getResult().getDeptIdList();
                    log.info("部门{}包含{}个子部门", currentDeptId, subDeptIds.size());

                    // 将子部门加入队列继续处理
                    for (Long subDeptId : subDeptIds) {
                        deptQueue.offer(subDeptId);
                    }
                } else {
                    if (response != null) {
                        log.error("获取部门{}的子部门ID失败，错误码：{}，错误信息：{}",
                                currentDeptId, response.getErrCode(), response.getErrMsg());
                    } else {
                        log.info("获取部门{}的子部门ID失败，响应为空", currentDeptId);
                    }
                }
            } catch (IOException | InterruptedException e) {
                log.error("获取部门{}的子部门ID失败", currentDeptId, e);
                Thread.currentThread().interrupt();
                break;
            }
        }
        return departmentIds;
    }


    /**
     * 应用API限流控制
     */
    private void applyRateLimit() throws InterruptedException {
        TimeUnit.MILLISECONDS.sleep(MIN_REQUEST_INTERVAL);
        apiRequestCount.incrementAndGet();
    }

    /**
     * 根据请求进度动态计算延迟时间
     *
     * @param requestIndex 请求索引
     * @return 延迟时间（毫秒）
     */
    private long calculateDelay(int requestIndex) {
        // 每10个请求增加额外延迟，确保不会触发限流
        if (requestIndex % 10 == 0) {
            return 200; // 额外200ms延迟
        }
        return MIN_REQUEST_INTERVAL;
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
            } else {
                if (response == null) {
                    log.info("获取部门详情失败，响应为空");
                } else {
                    log.error("获取部门详情失败，错误码：{}，错误信息：{}", response.getErrCode(), response.getErrMsg());
                }
            }

        } catch (IOException | InterruptedException e) {
            log.error("获取部门详情失败", e);
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
                    if (response == null) {
                        log.info("获取部门用户失败，响应为空");
                    } else {
                        log.error("获取部门用户失败，错误码：{}，错误信息：{}", response.getErrcode(), response.getErrmsg());
                    }
                }
            } catch (IOException | InterruptedException e) {
                log.error("获取部门用户失败", e);
                Thread.currentThread().interrupt();
                hasMore = false;
            }
        } while (hasMore);

        return users;
    }


}