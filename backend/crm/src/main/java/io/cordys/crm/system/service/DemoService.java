package io.cordys.crm.system.service;

import io.cordys.aspectj.annotation.OperationLog;
import io.cordys.aspectj.constants.LogModule;
import io.cordys.aspectj.constants.LogType;
import io.cordys.aspectj.context.OperationLogContext;
import io.cordys.aspectj.dto.LogExtraDTO;
import io.cordys.crm.system.domain.User;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class DemoService {

    @Resource
    private BaseMapper<User> userMapper;

    @OperationLog(
            module = LogModule.SYSTEM,
            type = LogType.ADD,
            operator = "{{#user.name}}",
            resourceId = "{{#user.id}}",
            success = "添加用户成功",
            extra = "{{#newUser}}"
    )
    public void addUser(User user) {
        // 添加用户
        user.setUpdateUser("admin");
        user.setCreateTime(System.currentTimeMillis());
        user.setUpdateTime(System.currentTimeMillis());
        user.setCreateUser("admin");
        user.setName("John Doe");
        user.setPhone("1234567890");
        user.setEmail("john.doe@example.com");
        user.setPassword("secure password");
        user.setEnable(1);
        user.setEmployeeId("E12345");
        user.setGender("Male");
        user.setPosition("Developer");
        user.setEmployeeType("Full-time");
        user.setSupervisorId("S12345");
        user.setWorkCity("New York");

        userMapper.insert(user);
        // 添加日志上下文
        OperationLogContext.putVariable("newUser", LogExtraDTO.builder()
                .originalValue(null)
                .modifiedValue(user)
                .build());
    }

    @OperationLog(
            module = LogModule.SYSTEM,
            type = LogType.DELETE,
            operator = "{{#userId}}",
            resourceId = "{{#userId}}",
            success = "删除用户成功",
            extra = "{{#delUser}}"
    )

    @CacheEvict(value = "users", key = "#userId")
    public void deleteUser(String userId) {
        // 删除用户
        User user = userMapper.selectByPrimaryKey(userId);

        // 添加日志上下文
        OperationLogContext.putVariable("delUser", LogExtraDTO.builder()
                .originalValue(user)
                .modifiedValue(null)
                .build());
    }

    @OperationLog(
            module = LogModule.SYSTEM,
            type = LogType.UPDATE,
            operator = "{{#user.name}}",
            resourceId = "{{#user.id}}",
            success = "更新用户成功",
            extra = "{{#upUser}}"
    )
    @OperationLog(
            module = LogModule.SYSTEM,
            type = LogType.UPDATE,
            operator = "{{#user.name}}",
            resourceId = "{{#user.id}}",
            success = "更新用户成功",
            extra = "{{#upUser}}"
    )

    @CachePut(value = "users", key = "#user.id", unless = "#result == null")
    public User updateUser(User user) {
        // 更新用户
        User preUser = userMapper.selectByPrimaryKey(user.getId());

        // 添加日志上下文
        OperationLogContext.putVariable("upUser", LogExtraDTO.builder()
                .originalValue(preUser)
                .modifiedValue(user)
                .build());

        return user;
    }

    @Cacheable(value = "users", key = "#userId", unless = "#result == null")
    public User getUser(String userId) {
        // 获取用户
        return userMapper.selectByPrimaryKey(userId);
    }

}
