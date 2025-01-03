package io.cordys.crm.system.service;

import io.cordys.common.util.BeanUtils;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

import io.cordys.crm.system.dto.request.*;
import io.cordys.crm.system.dto.response.*;
import io.cordys.crm.system.mapper.ExtRoleMapper;
import io.cordys.crm.system.domain.Role;

/**
 *
 * @author jianxing
 * @date 2025-01-03 12:01:54
 */
@Service("roleService")
@Transactional(rollbackFor = Exception.class)
public class RoleService {

    @Resource
    private BaseMapper<Role> RoleMapper;

    @Resource
    private ExtRoleMapper extRoleMapper;

    public List<RoleListResponse> list(RolePageRequest request) {
        List<RoleListResponse> list = extRoleMapper.list(request);
        // do something...
        return list;
    }


    public RoleGetResponse get(String id) {
        Role role = RoleMapper.selectByPrimaryKey(id);
        RoleGetResponse roleGetResponse = BeanUtils.copyBean(new RoleGetResponse(), role);
        // do something...
        return roleGetResponse;
    }

    public Role add(RoleAddRequest request, String userId) {
        Role role = BeanUtils.copyBean(new Role(), request);
        role.setCreateTime(System.currentTimeMillis());
        role.setUpdateTime(System.currentTimeMillis());
        role.setUpdateUser(userId);
        role.setCreateUser(userId);
        RoleMapper.insert(role);
        return role;
    }

    public Role update(RoleUpdateRequest request, String userId) {
        Role role = BeanUtils.copyBean(new Role(), request);
        role.setUpdateTime(System.currentTimeMillis());
        role.setUpdateUser(userId);
        RoleMapper.update(role);
        return RoleMapper.selectByPrimaryKey(role.getId());
    }

    public void delete(String id) {
        RoleMapper.deleteByPrimaryKey(id);
    }
}