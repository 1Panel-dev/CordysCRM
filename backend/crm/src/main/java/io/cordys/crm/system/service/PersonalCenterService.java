package io.cordys.crm.system.service;

import io.cordys.aspectj.annotation.OperationLog;
import io.cordys.aspectj.constants.LogModule;
import io.cordys.aspectj.constants.LogType;
import io.cordys.aspectj.context.OperationLogContext;
import io.cordys.aspectj.dto.LogContextInfo;
import io.cordys.common.exception.GenericException;
import io.cordys.common.util.CodingUtils;
import io.cordys.common.util.Translator;
import io.cordys.crm.system.domain.User;
import io.cordys.crm.system.dto.request.PersonalInfoRequest;
import io.cordys.crm.system.dto.request.PersonalPasswordRequest;
import io.cordys.crm.system.dto.response.UserResponse;
import io.cordys.crm.system.utils.MailSender;
import io.cordys.crm.system.mapper.ExtUserMapper;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class PersonalCenterService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private MailSender mailSender;

    @Resource
    private OrganizationUserService organizationUserService;

    @Resource
    private ExtUserMapper extUserMapper;

    @Resource
    private BaseMapper<User>userBaseMapper;


    private static final String PREFIX = "personal_email_code:";  // Redis 存储前缀


    public UserResponse getUserDetail(String id) {
        return organizationUserService.getUserDetail(id);
    }

    /**
     * 发送验证码
     */
    public void sendCode(String email, String organizationId) {
        if (stringRedisTemplate.hasKey(PREFIX + email)) {
            throw new GenericException(Translator.get("email_setting_reset_error"));
        }
        String code = generateCode();  // 生成随机验证码
        saveCode(email, code);         // 将验证码存入 Redis
        String[]users = new String[]{email};
        try {
            String emailSettingContent = Translator.get("email_setting_content");
            if (emailSettingContent.contains("${code}")) {
                emailSettingContent = emailSettingContent.replace("${code}", code);
            }
            mailSender.send(Translator.get("email_setting_subject"), emailSettingContent, users, new String[0], organizationId);      // 发送验证码到邮箱
        } catch (Exception e) {
            throw new GenericException(Translator.get("email_setting_send_error"));
        }
    }

    /**
     * 存储验证码到 Redis，设置有效期 5 分钟
     */
    private void saveCode(String email, String code) {
        stringRedisTemplate.opsForValue().set(PREFIX + email, code, 10, TimeUnit.MINUTES);
    }

    /**
     * 生成6位随机验证码
     */
    private String generateCode() {
        return String.valueOf(100000 + new Random().nextInt(900000));
    }

    /**
     * 验证码校验成功后删除
     * @param email email
     * @param code code
     * @return message
     */
    public boolean verifyCode(String email, String code) {
        boolean isVerify = false;
        String key = PREFIX + email;
        String correctCode = stringRedisTemplate.opsForValue().get(key);
        if (code != null && code.equals(correctCode)) {
            stringRedisTemplate.delete(key); // 验证通过后删除验证码
            isVerify = true;
        }
        return isVerify;

    }

    public void resetUserPassword(PersonalPasswordRequest personalPasswordRequest, String operatorId) {
        boolean verify = verifyCode(personalPasswordRequest.getEmail(), personalPasswordRequest.getCode());
        if (verify) {
            extUserMapper.updateUserPassword(CodingUtils.md5(personalPasswordRequest.getPassword()),operatorId);
        } else {
            throw new GenericException(Translator.get("email_setting_verify_error"));
        }
    }


    @OperationLog(module = LogModule.SYSTEM_DEPARTMENT_USER, type = LogType.UPDATE, operator = "{#userId}")
    public UserResponse updateInfo(PersonalInfoRequest personalInfoRequest, String userId) {
        User oldUser = userBaseMapper.selectByPrimaryKey(userId);
        User user = new User();
        user.setId(userId);
        user.setPhone(personalInfoRequest.getPhone());
        user.setEmail(personalInfoRequest.getEmail());
        userBaseMapper.update(user);

        UserResponse userDetail = organizationUserService.getUserDetail(userId);
        //添加日志上下文
        OperationLogContext.setContext(LogContextInfo.builder()
                .originalValue(oldUser)
                .resourceName(oldUser.getName())
                .modifiedValue(userDetail)
                .resourceId(userId)
                .build());

        return userDetail;
    }
}
