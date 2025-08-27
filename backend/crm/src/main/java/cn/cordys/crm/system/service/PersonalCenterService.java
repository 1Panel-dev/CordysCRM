package cn.cordys.crm.system.service;

import cn.cordys.aspectj.annotation.OperationLog;
import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.aspectj.dto.LogContextInfo;
import cn.cordys.common.constants.InternalUser;
import cn.cordys.common.constants.ModuleKey;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.CodingUtils;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.follow.dto.request.FollowUpPlanPageRequest;
import cn.cordys.crm.follow.dto.response.FollowUpPlanListResponse;
import cn.cordys.crm.follow.mapper.ExtFollowUpPlanMapper;
import cn.cordys.crm.follow.service.FollowUpPlanService;
import cn.cordys.crm.system.constants.NotificationConstants;
import cn.cordys.crm.system.domain.Module;
import cn.cordys.crm.system.domain.User;
import cn.cordys.crm.system.dto.request.PersonalInfoRequest;
import cn.cordys.crm.system.dto.request.PersonalPasswordRequest;
import cn.cordys.crm.system.dto.request.SendEmailDTO;
import cn.cordys.crm.system.dto.response.UserResponse;
import cn.cordys.crm.system.mapper.ExtOrganizationUserMapper;
import cn.cordys.crm.system.mapper.ExtUserMapper;
import cn.cordys.crm.system.mapper.ExtUserRoleMapper;
import cn.cordys.crm.system.utils.MailSender;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static cn.cordys.security.SessionUtils.kickOutUser;

@Service
public class PersonalCenterService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private MailSender mailSender;
    @Resource
    private OrganizationUserService organizationUserService;
    @Resource
    private FollowUpPlanService followUpPlanService;
    @Resource
    private ExtFollowUpPlanMapper extFollowUpPlanMapper;
    @Resource
    private ExtUserMapper extUserMapper;
    @Resource
    private BaseMapper<User> userBaseMapper;
    @Resource
    private ExtUserRoleMapper extUserRoleMapper;
    @Resource
    private BaseMapper<Module> moduleMapper;


    @Resource
    private ExtOrganizationUserMapper extOrganizationUserMapper;


    private static final String PREFIX = "personal_email_code:";  // Redis 存储前缀


    public UserResponse getUserDetail(String id, String orgId) {
        if (Strings.CS.equals(id, InternalUser.ADMIN.getValue())) {
            return BeanUtils.copyBean(new UserResponse(), userBaseMapper.selectByPrimaryKey(id));
        }
        String orgUserIdByUserId = extOrganizationUserMapper.getOrgUserIdByUserId(orgId, id);
        return organizationUserService.getUserDetail(orgUserIdByUserId);
    }

    /**
     * 发送验证码
     */
    public void sendCode(SendEmailDTO emailDTO, String organizationId) {
        String email = emailDTO.getEmail();
        String redisKey = PREFIX + email;
        if (stringRedisTemplate.hasKey(redisKey)) {
            stringRedisTemplate.delete(redisKey); // 验证通过后删除验证码
        }
        String code = generateCode();
        saveCode(email, code);

        try {
            String emailContent = Translator.get("email_setting_content")
                    .replace("${code}", code);

            mailSender.send(
                    Translator.get("email_setting_subject"),
                    emailContent,
                    new String[]{email},
                    new String[0],
                    organizationId
            );
        } catch (Exception e) {
            stringRedisTemplate.delete(redisKey); //
            throw new GenericException(e.getMessage());
        }
    }

    /**
     * 存储验证码到 Redis，设置有效期 10 分钟
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
     *
     * @param email email
     * @param code  code
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

    /**
     * 重置用户密码
     *
     * @param personalPasswordRequest 包含邮箱、验证码和新密码的请求对象
     * @param operatorId              操作者用户ID
     * @throws GenericException 验证码错误或密码重置失败时抛出
     */
    public void resetUserPassword(PersonalPasswordRequest personalPasswordRequest, String operatorId) {
        /*String email = personalPasswordRequest.getEmail();
        String code = personalPasswordRequest.getCode();*/
        String password = personalPasswordRequest.getPassword();

        // 验证邮箱验证码
      /*  if (!verifyCode(email, code)) {
            throw new GenericException(Translator.get("email_setting_verify_error"));
        }*/

        //检查原密码
        if (checkPwd(personalPasswordRequest.getOriginPassword(), operatorId)) {
            // 更新用户密码
            extUserMapper.updateUserPassword(CodingUtils.md5(password), operatorId);
            // 登出当前用户
            kickOutUser(operatorId, operatorId);
        } else {
            throw new GenericException(Translator.get("password_reset_error"));
        }
    }

    /**
     * 检查原密码
     *
     * @param originPassword
     * @param userId
     * @return
     */
    private boolean checkPwd(String originPassword, String userId) {
        User example = new User();
        example.setId(userId);
        example.setPassword(CodingUtils.md5(originPassword));
        return userBaseMapper.exist(example);
    }

    @OperationLog(module = LogModule.SYSTEM_ORGANIZATION, type = LogType.UPDATE, operator = "{#userId}")
    public UserResponse updateInfo(PersonalInfoRequest personalInfoRequest, String userId, String orgId) {
        User oldUser = userBaseMapper.selectByPrimaryKey(userId);
        int countByPhone = extUserMapper.countByPhone(personalInfoRequest.getPhone(), userId);
        if (countByPhone > 0) {
            throw new GenericException(Translator.get("phone.exist"));
        }
        int countByEmail = extUserMapper.countByEmail(personalInfoRequest.getEmail(), userId);
        if (countByEmail > 0) {
            throw new GenericException(Translator.get("email.exist"));
        }
        User user = new User();
        user.setId(userId);
        user.setPhone(personalInfoRequest.getPhone());
        user.setEmail(personalInfoRequest.getEmail());
        userBaseMapper.update(user);

        UserResponse userDetail = getUserDetail(userId, orgId);

        //添加日志上下文
        OperationLogContext.setContext(LogContextInfo.builder()
                .originalValue(oldUser)
                .resourceName(oldUser.getName())
                .modifiedValue(userDetail)
                .resourceId(userId)
                .build());

        return userDetail;
    }

    /**
     * 分页获取跟进计划列表
     *
     * @param request        分页查询请求参数
     * @param userId         当前用户ID
     * @param organizationId 组织ID
     * @return 分页的跟进计划列表及选项数据
     */
    public PagerWithOption<List<FollowUpPlanListResponse>> getPlanList(
            FollowUpPlanPageRequest request,
            String userId,
            String organizationId) {

        // 1. 获取用户权限和模块信息
        List<String> permissions = extUserRoleMapper.selectPermissionsByUserId(userId);
        boolean isAdmin = Strings.CI.equals(userId, InternalUser.ADMIN.getValue());

        // 2. 查询已启用的模块
        List<String> enabledModules = moduleMapper.selectListByLambda(
                        new LambdaQueryWrapper<Module>()
                                .eq(Module::getOrganizationId, organizationId)
                                .eq(Module::getEnable, true)
                ).stream()
                .map(Module::getModuleKey)
                .toList();

        // 3. 构建可访问的资源类型列表
        List<String> resourceTypeList = buildAccessibleResourceTypes(permissions, isAdmin, enabledModules);

        // 4. 初始化分页
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        // 5. 查询跟进计划基础数据
        if (CollectionUtils.isEmpty(resourceTypeList)) {
            return PageUtils.setPageInfoWithOption(page, new ArrayList<>(), new HashMap<>());

        }
        List<FollowUpPlanListResponse> planList = extFollowUpPlanMapper.selectList(
                request, userId, organizationId, null, null, null, resourceTypeList);

        // 6. 构建完整数据和选项映射
        List<FollowUpPlanListResponse> enrichedList = followUpPlanService.buildListData(planList, organizationId);
        Map<String, List<OptionDTO>> optionMap = followUpPlanService.buildOptionMap(organizationId, planList, enrichedList);

        // 7. 返回分页结果
        return PageUtils.setPageInfoWithOption(page, enrichedList, optionMap);
    }

    /**
     * 构建用户可访问的资源类型列表
     */
    private List<String> buildAccessibleResourceTypes(List<String> permissions,
                                                      boolean isAdmin,
                                                      List<String> enabledModules) {
        List<String> resourceTypes = new ArrayList<>();

        // 检查客户模块权限
        if ((permissions.contains(PermissionConstants.CUSTOMER_MANAGEMENT_READ) || isAdmin)
                && enabledModules.contains(ModuleKey.CUSTOMER.getKey())) {
            resourceTypes.add(NotificationConstants.Module.CUSTOMER);
        }

        // 检查商机模块权限
        if ((permissions.contains(PermissionConstants.OPPORTUNITY_MANAGEMENT_READ) || isAdmin)
                && enabledModules.contains(ModuleKey.BUSINESS.getKey())) {
            resourceTypes.add(NotificationConstants.Module.OPPORTUNITY);
        }

        // 检查线索模块权限
        if ((permissions.contains(PermissionConstants.CLUE_MANAGEMENT_READ) || isAdmin)
                && enabledModules.contains(ModuleKey.CLUE.getKey())) {
            resourceTypes.add(NotificationConstants.Module.CLUE);
        }

        return resourceTypes;
    }


}
