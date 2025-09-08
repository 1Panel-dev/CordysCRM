package cn.cordys.crm.system.service;

import cn.cordys.aspectj.builder.LogDTOBuilder;
import cn.cordys.aspectj.constants.LogConstants;
import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.aspectj.dto.LogContextInfo;
import cn.cordys.aspectj.dto.LogDTO;
import cn.cordys.common.constants.HttpMethodConstants;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.system.domain.UserKey;
import cn.cordys.crm.system.dto.request.UserKeyUpdateRequest;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
public class UserKeyService {

    @Resource
    private BaseMapper<UserKey> userKeyMapper;

    @Resource
    private UserLoginService userLoginService;

    @Resource
    private LogService logService;

    /**
     * 获取指定用户的 API 密钥信息
     */
    public List<UserKey> getUserKeysInfo(String userId) {
        UserKey example = new UserKey();
        example.setCreateUser(userId);
        return userKeyMapper.select(example);
    }

    /**
     * 为指定用户添加 API 密钥
     */
    public void addUserKey(String userId) {
        validateUserExistence(userId);

        List<UserKey> userKeysList = getUserKeysByUserId(userId);

        // 如果该用户已有 5 个密钥，抛出限制异常
        if (!CollectionUtils.isEmpty(userKeysList) && userKeysList.size() >= 5) {
            throw new GenericException(Translator.get("user_apikey_limit"));
        }

        UserKey userKey = generateUserKey(userId);
        userKeyMapper.insert(userKey);

        logApiKeyAction(userKey, LogType.ADD);
    }

    /**
     * 删除指定 ID 的 API 密钥
     */
    public void deleteUserKey(String id) {
        UserKey userKey = validateAndGetUserKey(id);
        userKeyMapper.deleteByPrimaryKey(userKey.getId());
    }

    /**
     * 启用指定 ID 的 API 密钥
     */
    public void enableUserKey(String id) {
        UserKey userKey = validateAndGetUserKey(id);
        updateUserKeyStatus(userKey, true);
    }

    /**
     * 禁用指定 ID 的 API 密钥
     */
    public void disableUserKey(String id) {
        UserKey userKey = validateAndGetUserKey(id);
        updateUserKeyStatus(userKey, false);
    }

    /**
     * 根据 accessKey 获取 API 密钥信息
     */
    public UserKey getUserKey(String accessKey) {
        UserKey example = new UserKey();
        example.setAccessKey(accessKey);
        example.setEnable(true);
        List<UserKey> userKeysList = userKeyMapper.select(example);
        return CollectionUtils.isEmpty(userKeysList) ? null : userKeysList.getFirst();
    }

    /**
     * 校验 API 密钥是否存在
     */
    private UserKey validateAndGetUserKey(String id) {
        UserKey userKey = userKeyMapper.selectByPrimaryKey(id);
        if (userKey == null) {
            throw new GenericException(Translator.get("api_key_not_exist"));
        }
        return userKey;
    }

    /**
     * 校验用户是否存在
     */
    private void validateUserExistence(String userId) {
        if (userLoginService.authenticateUser(userId) == null) {
            throw new GenericException(Translator.get("user_not_exist") + userId);
        }
    }

    /**
     * 获取指定用户的所有 API 密钥
     */
    private List<UserKey> getUserKeysByUserId(String userId) {
        UserKey example = new UserKey();
        example.setCreateUser(userId);
        return userKeyMapper.select(example);
    }

    /**
     * 生成新的 API 密钥
     */
    private UserKey generateUserKey(String userId) {
        UserKey userKey = new UserKey();
        userKey.setId(IDGenerator.nextStr());
        userKey.setCreateUser(userId);
        userKey.setEnable(true);
        userKey.setAccessKey(RandomStringUtils.secure().nextAlphabetic(16));
        userKey.setSecretKey(RandomStringUtils.secure().nextAlphabetic(16));
        userKey.setCreateTime(System.currentTimeMillis());
        userKey.setForever(true);
        return userKey;
    }

    /**
     * 更新 API 密钥的状态（启用/禁用）
     */
    private void updateUserKeyStatus(UserKey userKey, boolean enable) {
        userKey.setEnable(enable);
        userKeyMapper.updateById(userKey);
    }

    /**
     * 记录 API 密钥操作日志
     */
    private void logApiKeyAction(UserKey userKey, String actionType) {
        LogDTO logDTO = LogDTOBuilder.builder()
                .organizationId(LogConstants.SYSTEM)
                .type(actionType)
                .module(LogModule.PERSONAL_INFORMATION_APIKEY)
                .method(HttpMethodConstants.GET.name())
                .path("/user/api/key/add")
                .sourceId(userKey.getId())
                .content(userKey.getAccessKey())
                .build()
                .getLogDTO();

        OperationLogContext.setContext(
                LogContextInfo.builder()
                        .modifiedValue(userKey)
                        .build()
        );
        logService.add(logDTO);
    }


    public void updateUserKey(UserKeyUpdateRequest request) {
        UserKey userKey = validateAndGetUserKey(request.getId());
        userKey.setId(request.getId());
        userKey.setForever(request.getForever());
        if (BooleanUtils.isFalse(request.getForever())) {
            if (request.getExpireTime() == null) {
                throw new GenericException(Translator.get("expire_time_not_null"));
            }
            userKey.setExpireTime(request.getExpireTime());
        } else {
            userKey.setExpireTime(null);
        }
        userKey.setDescription(request.getDescription());
        userKeyMapper.updateById(userKey);
    }
}
