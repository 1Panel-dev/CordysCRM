package cn.cordys.crm.integration.common.utils;

import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.crm.integration.dingtalk.service.DingTalkDepartmentService;
import cn.cordys.crm.integration.lark.service.LarkDepartmentService;
import cn.cordys.crm.integration.sync.dto.ThirdDepartment;
import cn.cordys.crm.integration.sync.dto.ThirdOrgDataDTO;
import cn.cordys.crm.integration.sync.dto.ThirdUser;
import cn.cordys.crm.integration.wecom.service.WeComDepartmentService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DepartmentHandleUtils {

    private final WeComDepartmentService weComDepartmentService;
    private final DingTalkDepartmentService dingTalkDepartmentService;
    private final LarkDepartmentService larkDepartmentService;

    public DepartmentHandleUtils() {
        this.weComDepartmentService = CommonBeanFactory.getBean(WeComDepartmentService.class);
        this.dingTalkDepartmentService = CommonBeanFactory.getBean(DingTalkDepartmentService.class);
        this.larkDepartmentService = CommonBeanFactory.getBean(LarkDepartmentService.class);
    }


    /**
     * 企业微信：获取部门信息
     *
     * @param accessToken
     * @param wecomDepartmentIds
     * @return
     */
    public List<ThirdDepartment> handleWecom(String accessToken, List<String> wecomDepartmentIds) {
        List<ThirdDepartment> depatmentList = new ArrayList<>();
        for (String wecomDepartmentId : wecomDepartmentIds) {
            depatmentList.addAll(weComDepartmentService.getDepartmentList(accessToken, wecomDepartmentId));
        }

        return depatmentList.stream()
                .collect(Collectors.toMap(
                        ThirdDepartment::getId,
                        Function.identity(),
                        (a, b) -> a
                ))
                .values()
                .stream()
                .toList();

    }


    /**
     * 钉钉：获取部门信息
     *
     * @param accessToken
     * @param dingTalkDepartmentIds
     * @return
     */
    public ThirdOrgDataDTO handleDingTalk(String accessToken, List<String> dingTalkDepartmentIds) {
        ThirdOrgDataDTO thirdOrgDataDTO = new ThirdOrgDataDTO();
        List<ThirdDepartment> departments = new ArrayList<>();
        Map<String, List<ThirdUser>> users = new HashMap<>();
        for (String dingTalkDepartmentId : dingTalkDepartmentIds) {
            ThirdOrgDataDTO response = dingTalkDepartmentService.convertToThirdOrgDataDTO(accessToken, dingTalkDepartmentId, true);
            departments.addAll(response.getDepartments());
            users.putAll(response.getUsers());
        }
        thirdOrgDataDTO.setDepartments(departments.stream().collect(Collectors.toMap(ThirdDepartment::getId, Function.identity(), (a, b) -> a)).values().stream().toList());
        thirdOrgDataDTO.setUsers(users);
        return thirdOrgDataDTO;
    }


    /**
     * 飞书：获取部门信息
     *
     * @param accessToken
     * @param larkDepartmentIds
     * @return
     */
    public List<ThirdDepartment> handleLark(String accessToken, List<String> larkDepartmentIds, List<ThirdDepartment> allDepartmentList) {
        List<ThirdDepartment> depatmentList = new ArrayList<>();
        for (String departmentId : larkDepartmentIds) {
            depatmentList.addAll(larkDepartmentService.getDepartmentListById(accessToken, departmentId, allDepartmentList));
        }

        return depatmentList.stream()
                .collect(Collectors.toMap(
                        ThirdDepartment::getId,
                        Function.identity(),
                        (a, b) -> a
                ))
                .values()
                .stream()
                .toList();
    }
}
