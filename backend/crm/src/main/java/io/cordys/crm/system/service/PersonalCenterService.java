package io.cordys.crm.system.service;

import io.cordys.aspectj.annotation.OperationLog;
import io.cordys.aspectj.constants.LogModule;
import io.cordys.aspectj.constants.LogType;
import io.cordys.aspectj.context.OperationLogContext;
import io.cordys.aspectj.dto.LogContextInfo;
import io.cordys.common.exception.GenericException;
import io.cordys.common.util.CodingUtils;
import io.cordys.common.util.Translator;
import io.cordys.crm.system.dto.response.UserResponse;
import io.cordys.crm.system.mail.sender.MailSender;
import io.cordys.crm.system.mapper.ExtUserMapper;
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
    public String verifyCode(String email, String code) {
        boolean isValid = false;
        String key = PREFIX + email;
        String correctCode = stringRedisTemplate.opsForValue().get(key);
        if (code != null && code.equals(correctCode)) {
            stringRedisTemplate.delete(key); // 验证通过后删除验证码
            isValid = true;
        }
        return isValid ? Translator.get("email_setting_verify_success") : Translator.get("email_setting_verify_error");

    }

    @OperationLog(module = LogModule.SYSTEM_DEPARTMENT_USER, type = LogType.UPDATE, operator = "{#operatorId}")
    public void resetUserPassword(String password, String operatorId) {
        UserResponse oldUser = getUserDetail(operatorId);
        extUserMapper.updateUserPassword(CodingUtils.md5(password),operatorId);
        //添加日志上下文
        OperationLogContext.setContext(LogContextInfo.builder()
                .originalValue(oldUser)
                .resourceName(oldUser.getUserName())
                .modifiedValue(getUserDetail(operatorId))
                .resourceId(oldUser.getId())
                .build());
    }



}
