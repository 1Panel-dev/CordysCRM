package io.cordys.crm.system.service;

import io.cordys.common.dto.OptionDTO;
import io.cordys.crm.system.dto.convert.UserRoleConvert;
import io.cordys.crm.system.dto.request.UserPageRequest;
import io.cordys.crm.system.dto.response.UserPageResponse;
import io.cordys.crm.system.mapper.ExtOrganizationUserMapper;
import io.cordys.crm.system.mapper.ExtUserMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service("organizationUserService")
@Transactional(rollbackFor = Exception.class)
public class OrganizationUserService {

    @Resource
    private ExtOrganizationUserMapper extOrganizationUserMapper;
    @Resource
    private ExtUserMapper extUserMapper;


    /**
     * 员工列表查询
     *
     * @param request
     * @return
     */
    public List<UserPageResponse> list(UserPageRequest request) {
        List<UserPageResponse> list = extOrganizationUserMapper.list(request);
        handleData(list);
        return list;
    }


    private void handleData(List<UserPageResponse> list) {
        if (CollectionUtils.isNotEmpty(list)) {
            String orgId = list.stream().map(UserPageResponse::getOrganizationId).toList().getFirst();
            List<String> userIds = list.stream().map(UserPageResponse::getUserId).toList();
            //获取用户角色
            List<UserRoleConvert> userRoles = extUserMapper.getUserRole(userIds, orgId);
            Map<String, List<UserRoleConvert>> userRoleMap = userRoles.stream().collect(Collectors.groupingBy(UserRoleConvert::getUserId));
            //创建人 更新人
            List<String> ids = new ArrayList<>();
            ids.addAll(list.stream().map(UserPageResponse::getCreateUser).toList());
            ids.addAll(list.stream().map(UserPageResponse::getUpdateUser).toList());
            List<OptionDTO> optionDTOS = extUserMapper.selectUserOptionByIds(ids);
            Map<String, String> userMap = optionDTOS.stream().collect(Collectors.toMap(OptionDTO::getId, OptionDTO::getName));

            //todo 暂无用户组 后续需求再处理
            list.forEach(user -> {
                if (userRoleMap.containsKey(user.getUserId())) {
                    user.setRoles(userRoleMap.get(user.getUserId()));
                }
                if (userMap.containsKey(user.getCreateUser())) {
                    user.setCreateUserName(userMap.get(user.getCreateUser()));
                }
                if (userMap.containsKey(user.getUpdateUser())) {
                    user.setUpdateUserName(userMap.get(user.getUpdateUser()));
                }
            });

        }
    }
}