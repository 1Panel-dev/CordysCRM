package io.cordys.crm.system.service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.cordys.aspectj.annotation.OperationLog;
import io.cordys.aspectj.constants.LogModule;
import io.cordys.aspectj.constants.LogType;
import io.cordys.aspectj.context.OperationLogContext;
import io.cordys.aspectj.dto.LogContextInfo;
import io.cordys.common.constants.InternalUser;
import io.cordys.common.constants.ModuleKey;
import io.cordys.common.constants.PermissionConstants;
import io.cordys.common.dto.OptionDTO;
import io.cordys.common.exception.GenericException;
import io.cordys.common.pager.PageUtils;
import io.cordys.common.pager.Pager;
import io.cordys.common.pager.PagerWithOption;
import io.cordys.common.util.BeanUtils;
import io.cordys.common.util.CodingUtils;
import io.cordys.common.util.Translator;
import io.cordys.context.OrganizationContext;
import io.cordys.crm.clue.dto.response.ClueRepeatListResponse;
import io.cordys.crm.clue.mapper.ExtClueMapper;
import io.cordys.crm.customer.dto.response.CustomerRepeatResponse;
import io.cordys.crm.customer.mapper.ExtCustomerMapper;
import io.cordys.crm.follow.dto.request.FollowUpPlanPageRequest;
import io.cordys.crm.follow.dto.response.FollowUpPlanListResponse;
import io.cordys.crm.follow.mapper.ExtFollowUpPlanMapper;
import io.cordys.crm.follow.service.FollowUpPlanService;
import io.cordys.crm.opportunity.dto.response.OpportunityRepeatResponse;
import io.cordys.crm.opportunity.mapper.ExtOpportunityMapper;
import io.cordys.crm.system.constants.NotificationConstants;
import io.cordys.crm.system.constants.RepeatType;
import io.cordys.crm.system.constants.SystemResultCode;
import io.cordys.crm.system.domain.Module;
import io.cordys.crm.system.domain.Product;
import io.cordys.crm.system.domain.User;
import io.cordys.crm.system.dto.request.*;
import io.cordys.crm.system.dto.response.UserResponse;
import io.cordys.crm.system.mapper.ExtOrganizationUserMapper;
import io.cordys.crm.system.mapper.ExtProductMapper;
import io.cordys.crm.system.mapper.ExtUserMapper;
import io.cordys.crm.system.mapper.ExtUserRoleMapper;
import io.cordys.crm.system.utils.MailSender;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.cordys.security.SessionUtils.kickOutUser;

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
    private ExtUserMapper extUserMapper;
    @Resource
    private BaseMapper<User> userBaseMapper;
    @Resource
    private ExtCustomerMapper extCustomerMapper;
    @Resource
    private ExtOpportunityMapper extOpportunityMapper;
    @Resource
    private ExtProductMapper extProductMapper;
    @Resource
    private ExtClueMapper extClueMapper;
    @Resource
    private ExtOrganizationUserMapper extOrganizationUserMapper;
    @Resource
    private ExtFollowUpPlanMapper extFollowUpPlanMapper;
    @Resource
    private ExtUserRoleMapper extUserRoleMapper;
    @Resource
    private BaseMapper<Module> moduleMapper;

    private static final String PREFIX = "personal_email_code:";  // Redis 存储前缀


    public UserResponse getUserDetail(String id, String orgId) {
        if (StringUtils.equals(id, InternalUser.ADMIN.getValue())) {
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
        String email = personalPasswordRequest.getEmail();
        String code = personalPasswordRequest.getCode();
        String password = personalPasswordRequest.getPassword();

        // 验证邮箱验证码
        if (!verifyCode(email, code)) {
            throw new GenericException(Translator.get("email_setting_verify_error"));
        }

        try {
            // 更新用户密码
            extUserMapper.updateUserPassword(CodingUtils.md5(password), operatorId);
            // 登出当前用户
            kickOutUser(operatorId, operatorId);
        } catch (Exception e) {
            // 记录异常并重新抛出
            throw new GenericException(Translator.get("password_reset_failed"), e);
        }
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
     * 获取重复客户列表
     *
     * @param request        查询请求参数
     * @param organizationId 组织ID
     * @param userId         用户ID
     * @return 重复客户响应列表
     */
    public Pager<List<CustomerRepeatResponse>> getRepeatCustomer(RepeatCustomerPageRequest request,
                                                                 String organizationId,
                                                                 String userId) {
        // 如果客户名为空，直接返回空列表
        if (StringUtils.isBlank(request.getName())) {
            return PageUtils.setPageInfo(
                    PageHelper.startPage(request.getCurrent(), request.getPageSize()), new ArrayList<>());
        }

        // 查询用户权限并检查是否是管理员
        List<String> permissions = extUserRoleMapper.selectPermissionsByUserId(userId);
        boolean isAdmin = StringUtils.equalsIgnoreCase(userId, InternalUser.ADMIN.getValue());
        boolean hasCustomerRead = permissions.contains(PermissionConstants.CUSTOMER_MANAGEMENT_READ) || isAdmin;

        // 查询当前组织下已启用的模块列表
        List<String> enabledModules = moduleMapper.selectListByLambda(
                        new LambdaQueryWrapper<Module>()
                                .eq(Module::getOrganizationId, OrganizationContext.getOrganizationId())
                                .eq(Module::getEnable, true)
                ).stream()
                .map(Module::getModuleKey)
                .toList();

        // 检查：如果有客户读取权限但客户模块未启用，抛出异常
        if (hasCustomerRead && !enabledModules.contains(ModuleKey.CUSTOMER.getKey())) {
            throw new GenericException(SystemResultCode.MODULE_ENABLE);
        }

        // 没有客户读取权限直接返回空列表
        if (!hasCustomerRead) {
            return PageUtils.setPageInfo(
                    PageHelper.startPage(request.getCurrent(), request.getPageSize()), new ArrayList<>());
        }

        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        // 查询重复客户列表
        List<CustomerRepeatResponse> customers = extCustomerMapper.checkRepeatCustomerByName(request.getName(), organizationId);
        if (CollectionUtils.isEmpty(customers)) {
            return PageUtils.setPageInfo(
                    PageHelper.startPage(request.getCurrent(), request.getPageSize()), new ArrayList<>());
        }

        // 获取商机和线索的重复数量映射
        Map<String, String> opportunityCounts = getOpportunityCounts(permissions, isAdmin, customers, enabledModules);
        Map<String, String> clueCounts = getClueCounts(permissions, isAdmin, customers, enabledModules);

        // 检查相关模块是否启用
        boolean isClueModuleEnabled = enabledModules.contains(ModuleKey.CLUE.getKey());
        boolean isOpportunityModuleEnabled = enabledModules.contains(ModuleKey.BUSINESS.getKey());

        // 处理每个客户返回结果
        return PageUtils.setPageInfo(page, customers.stream()
                .peek(customer -> {
                    // 设置重复类型(完全匹配/部分匹配)
                    customer.setRepeatType(StringUtils.equalsIgnoreCase(customer.getName(), request.getName())
                            ? RepeatType.ALL.toString()
                            : RepeatType.PART.toString());
                    // 设置商机数量
                    customer.setOpportunityCount(parseCount(opportunityCounts.get(customer.getId())));
                    // 设置线索数量
                    customer.setClueCount(parseCount(clueCounts.get(customer.getName())));
                    // 设置模块启用状态
                    customer.setOpportunityModuleEnable(isOpportunityModuleEnabled);
                    customer.setClueModuleEnable(isClueModuleEnabled);
                })
                .toList());
    }

    /**
     * 获取客户关联的商机数量
     *
     * @param permissions    用户权限列表
     * @param isAdmin        是否是管理员
     * @param customers      客户列表
     * @param enabledModules 已启用模块列表
     * @return 商机数量映射(客户ID - > 数量)
     */
    private Map<String, String> getOpportunityCounts(List<String> permissions,
                                                     boolean isAdmin,
                                                     List<CustomerRepeatResponse> customers,
                                                     List<String> enabledModules) {
        // 没有商机读取权限且不是管理员返回空map
        if (!permissions.contains(PermissionConstants.OPPORTUNITY_MANAGEMENT_READ) && !isAdmin) {
            return Collections.emptyMap();
        }
        // 商机模块未启用返回空map
        if (!enabledModules.contains(ModuleKey.BUSINESS.getKey())) {
            return Collections.emptyMap();
        }

        // 获取客户ID列表并查询商机数量
        List<String> customerIds = customers.stream()
                .map(CustomerRepeatResponse::getId)
                .toList();

        return extOpportunityMapper.getRepeatCountMap(customerIds).stream()
                .collect(Collectors.toMap(OptionDTO::getId, OptionDTO::getName));
    }

    /**
     * 获取客户关联的线索数量
     *
     * @param permissions    用户权限列表
     * @param isAdmin        是否是管理员
     * @param customers      客户列表
     * @param enabledModules 已启用模块列表
     * @return 线索数量映射(客户名称 - > 数量)
     */
    private Map<String, String> getClueCounts(List<String> permissions,
                                              boolean isAdmin,
                                              List<CustomerRepeatResponse> customers,
                                              List<String> enabledModules) {
        // 没有线索读取权限且不是管理员返回空map
        if (!permissions.contains(PermissionConstants.CLUE_MANAGEMENT_READ) && !isAdmin) {
            return Collections.emptyMap();
        }
        // 线索模块未启用返回空map
        if (!enabledModules.contains(ModuleKey.CLUE.getKey())) {
            return Collections.emptyMap();
        }

        // 获取客户名称列表并查询线索数量
        List<String> customerNames = customers.stream()
                .map(CustomerRepeatResponse::getName)
                .toList();

        return extClueMapper.getRepeatCountMap(customerNames).stream()
                .collect(Collectors.toMap(OptionDTO::getId, OptionDTO::getName));
    }

    /**
     * 安全解析数量字符串
     *
     * @param count 数量字符串
     * @return 解析后的整型数量(空字符串返回0)
     */
    private int parseCount(String count) {
        return StringUtils.isBlank(count) ? 0 : Integer.parseInt(count);
    }

    /**
     * 获取重复线索列表
     *
     * @param request        查询请求参数(包含线索名称)
     * @param organizationId 组织ID
     * @param userId         用户ID
     * @return 重复线索响应列表
     */
    public Pager<List<ClueRepeatListResponse>> getRepeatClue(RepeatCustomerPageRequest request,
                                                             String organizationId,
                                                             String userId) {
        if (StringUtils.isBlank(request.getName())) {
            return PageUtils.setPageInfo(
                    PageHelper.startPage(request.getCurrent(), request.getPageSize()), new ArrayList<>());
        }

        // 1. 获取用户权限列表
        List<String> permissions = extUserRoleMapper.selectPermissionsByUserId(userId);

        // 2. 检查线索模块是否启用
        LambdaQueryWrapper<Module> moduleQuery = new LambdaQueryWrapper<>();
        moduleQuery.eq(Module::getOrganizationId, organizationId)
                .eq(Module::getEnable, true)
                .eq(Module::getModuleKey, ModuleKey.CLUE.getKey());
        List<Module> modules = moduleMapper.selectListByLambda(moduleQuery);

        // 3. 权限检查：有线索读取权限或是管理员，但线索模块未启用
        boolean hasClueReadPermission = permissions.contains(PermissionConstants.CLUE_MANAGEMENT_READ);
        boolean isAdmin = StringUtils.equalsIgnoreCase(userId, InternalUser.ADMIN.getValue());

        if ((hasClueReadPermission || isAdmin) && CollectionUtils.isEmpty(modules)) {
            throw new GenericException(SystemResultCode.MODULE_ENABLE);
        }

        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        // 4. 查询并返回相似线索列表
        return PageUtils.setPageInfo(page, extClueMapper.getSimilarClueList(request.getName(), organizationId));
    }

    public List<ClueRepeatListResponse> getRepeatClueDetail(RepeatCustomerDetailPageRequest request,
                                                            String organizationId) {
        return extClueMapper.getRepeatClueList(request.getName(), organizationId);
    }

    /**
     * 获取重复商机详情列表
     *
     * @param request 包含商机ID的查询请求
     * @return 重复商机响应列表(包含产品名称信息)
     */
    public List<OpportunityRepeatResponse> getRepeatOpportunityDetail(
            RepeatCustomerDetailPageRequest request) {

        // 1. 获取基础重复商机列表
        List<OpportunityRepeatResponse> responses = extOpportunityMapper.getRepeatList(request.getId());

        if (CollectionUtils.isEmpty(responses)) {
            return responses;
        }

        // 2. 获取所有不重复的产品ID列表
        List<String> productIds = responses.stream()
                .flatMap(response -> response.getProducts().stream())
                .distinct()
                .toList();

        // 3. 批量获取产品名称映射(优化数据库查询)
        Map<String, String> productNameMap = productIds.isEmpty()
                ? Collections.emptyMap()
                : extProductMapper.listIdNameByIds(productIds).stream()
                .collect(Collectors.toMap(
                        Product::getId,
                        Product::getName,
                        (existing, replacement) -> existing)); // 处理可能的重复键

        // 4. 填充每个商机的产品名称
        responses.forEach(response -> {
            List<String> names = response.getProducts().stream()
                    .map(productNameMap::get)
                    .filter(Objects::nonNull) // 过滤掉null值
                    .toList();
            response.setProductNames(names);
        });

        return responses;
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
        boolean isAdmin = StringUtils.equalsIgnoreCase(userId, InternalUser.ADMIN.getValue());

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
