package cn.cordys.crm.integration.wecom.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.LogUtils;
import cn.cordys.crm.integration.common.utils.HttpRequestUtil;
import cn.cordys.crm.integration.sync.dto.ThirdDepartment;
import cn.cordys.crm.integration.sync.dto.ThirdUser;
import cn.cordys.crm.integration.wecom.constant.WeComApiPaths;
import cn.cordys.crm.integration.wecom.response.WeComDepartmentListResponse;
import cn.cordys.crm.integration.wecom.response.WeComUserListResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.cordys.crm.integration.common.utils.HttpRequestUtil.urlTransfer;

@Service
@Transactional(rollbackFor = Exception.class)
public class WeComDepartmentService {

    /**
     * 获取部门列表
     *
     * @param accessToken 访问令牌
     * @return 部门列表
     */
    public List<ThirdDepartment> getDepartmentList(String accessToken) {
        String url = urlTransfer(WeComApiPaths.DEPARTMENT_LIST, accessToken, null);
        WeComDepartmentListResponse response = fetchDepartmentList(url);

        if (response.getErrCode() != 0) {
            throw new GenericException("获取部门接口返回结果失败: " + response.getErrMsg());
        }

        return response.getDepartment().stream().map(dept -> {
            ThirdDepartment thirdDept = new ThirdDepartment();
            thirdDept.setId(dept.getId().toString());
            thirdDept.setName(dept.getName());
            thirdDept.setParentId(dept.getParentId().toString());
            thirdDept.setOrder(dept.getOrder());
            thirdDept.setIsRoot(dept.getId() == 1);
            return thirdDept;
        }).toList();
    }

    /**
     * 获取部门列表
     */
    private WeComDepartmentListResponse fetchDepartmentList(String url) {
        try {
            String responseStr = HttpRequestUtil.sendGetRequest(url, null);
            return JSON.parseObject(responseStr, WeComDepartmentListResponse.class);
        } catch (Exception e) {
            LogUtils.error(e);
            throw new GenericException("调用接口获取部门列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取部门用户详情
     *
     * @param accessToken   访问令牌
     * @param departmentIds 部门ID列表
     * @return 部门ID与用户列表的映射
     */
    public Map<String, List<ThirdUser>> getDepartmentUser(String accessToken, List<Long> departmentIds) {
        Map<String, List<ThirdUser>> thirdUserMap = new HashMap<>();

        for (Long departmentId : departmentIds) {
            String url = urlTransfer(WeComApiPaths.USER_LIST, accessToken, departmentId);
            WeComUserListResponse response = fetchUserList(url);

            if (response.getErrCode() != 0) {
                throw new GenericException("获取用户接口返回结果失败: " + response.getErrMsg());
            }
            List<ThirdUser> thirdUsers = response.getUserList().stream()//过滤如果weComUser有mainDepartment属性，过滤department中含有mainDepartment的用户，没有就不过滤
                    .filter(weComUser -> weComUser.getMainDepartment() == null || weComUser.getMainDepartment().equals(departmentId))
                    .map(weComUser -> ThirdUser.builder()
                            .userId(weComUser.getUserId())
                            .name(weComUser.getName())
                            .email(weComUser.getEmail())
                            .mobile(weComUser.getMobile())
                            .position(weComUser.getPosition())
                            .isLeaderInDept(weComUser.getIsLeaderInDept().indexOf(Integer.parseInt(departmentId.toString())) > 0)
                            .build())
                    .collect(Collectors.toList());


            thirdUserMap.put(departmentId.toString(), thirdUsers);
        }

        return thirdUserMap;
    }


    /**
     * 获取用户列表
     */
    private WeComUserListResponse fetchUserList(String url) {
        try {
            String responseStr = HttpRequestUtil.sendGetRequest(url, null);
            return JSON.parseObject(responseStr, WeComUserListResponse.class);
        } catch (Exception e) {
            throw new GenericException("调用接口获取用户列表失败", e);
        }
    }
}
