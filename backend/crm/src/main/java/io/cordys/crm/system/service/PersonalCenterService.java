package io.cordys.crm.system.service;

import io.cordys.aspectj.annotation.OperationLog;
import io.cordys.aspectj.constants.LogModule;
import io.cordys.aspectj.constants.LogType;
import io.cordys.aspectj.context.OperationLogContext;
import io.cordys.aspectj.dto.LogContextInfo;
import io.cordys.common.constants.BusinessSearchType;
import io.cordys.common.constants.InternalUser;
import io.cordys.common.constants.PermissionConstants;
import io.cordys.common.dto.DeptDataPermissionDTO;
import io.cordys.common.exception.GenericException;
import io.cordys.common.util.BeanUtils;
import io.cordys.common.util.CodingUtils;
import io.cordys.common.util.Translator;
import io.cordys.crm.clue.dto.request.CluePageRequest;
import io.cordys.crm.clue.dto.response.ClueListResponse;
import io.cordys.crm.clue.mapper.ExtClueMapper;
import io.cordys.crm.customer.domain.Customer;
import io.cordys.crm.customer.dto.response.CustomerRepeatResponse;
import io.cordys.crm.customer.mapper.ExtCustomerMapper;
import io.cordys.crm.opportunity.dto.response.OpportunityRepeatResponse;
import io.cordys.crm.opportunity.mapper.ExtOpportunityMapper;
import io.cordys.crm.system.domain.Product;
import io.cordys.crm.system.domain.User;
import io.cordys.crm.system.dto.request.PersonalInfoRequest;
import io.cordys.crm.system.dto.request.PersonalPasswordRequest;
import io.cordys.crm.system.dto.response.RepeatCustomerResponse;
import io.cordys.crm.system.dto.response.UserResponse;
import io.cordys.crm.system.mapper.ExtOrganizationUserMapper;
import io.cordys.crm.system.mapper.ExtProductMapper;
import io.cordys.crm.system.mapper.ExtUserMapper;
import io.cordys.crm.system.mapper.ExtUserRoleMapper;
import io.cordys.crm.system.utils.MailSender;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
    private BaseMapper<User> userBaseMapper;

    @Resource
    private ExtUserRoleMapper extUserRoleMapper;

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


    private static final String PREFIX = "personal_email_code:";  // Redis 存储前缀


    public UserResponse getUserDetail(String id, String orgId) {
        String orgUserIdByUserId = extOrganizationUserMapper.getOrgUserIdByUserId(orgId, id);
        return organizationUserService.getUserDetail(orgUserIdByUserId);
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
        String[] users = new String[]{email};
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

    public void resetUserPassword(PersonalPasswordRequest personalPasswordRequest, String operatorId) {
        boolean verify = verifyCode(personalPasswordRequest.getEmail(), personalPasswordRequest.getCode());
        if (verify) {
            extUserMapper.updateUserPassword(CodingUtils.md5(personalPasswordRequest.getPassword()), operatorId);
        } else {
            throw new GenericException(Translator.get("email_setting_verify_error"));
        }
    }


    @OperationLog(module = LogModule.SYSTEM_DEPARTMENT_USER, type = LogType.UPDATE, operator = "{#userId}")
    public UserResponse updateInfo(PersonalInfoRequest personalInfoRequest, String userId, String orgId) {
        User oldUser = userBaseMapper.selectByPrimaryKey(userId);
        User user = new User();
        user.setId(userId);
        user.setPhone(personalInfoRequest.getPhone());
        user.setEmail(personalInfoRequest.getEmail());
        userBaseMapper.update(user);

        String orgUserIdByUserId = extOrganizationUserMapper.getOrgUserIdByUserId(orgId, userId);
        UserResponse userDetail = organizationUserService.getUserDetail(orgUserIdByUserId);
        //添加日志上下文
        OperationLogContext.setContext(LogContextInfo.builder()
                .originalValue(oldUser)
                .resourceName(oldUser.getName())
                .modifiedValue(userDetail)
                .resourceId(userId)
                .build());

        return userDetail;
    }

    public RepeatCustomerResponse getRepeatCustomer(String organizationId, String userId, String name) {
        RepeatCustomerResponse repeatCustomerResponse = new RepeatCustomerResponse();
        //1.查询当前用户权限
        List<String> permissions = extUserRoleMapper.selectPermissionsByUserId(userId);
        //2.根据权限查询客户，线索，商机数据
        List<CustomerRepeatResponse> customerRepeatResponses = new ArrayList<>();
        if (permissions.indexOf(PermissionConstants.CUSTOMER_MANAGEMENT_READ) > 0 || StringUtils.equalsIgnoreCase(userId, InternalUser.ADMIN.getValue())) {
            List<Customer> customers = extCustomerMapper.checkRepeatCustomerByName(name, organizationId);
            if (CollectionUtils.isNotEmpty(customers)) {
                List<String> ownerIds = customers.stream().map(Customer::getOwner).collect(Collectors.toList());
                Map<String, UserResponse> userResponseMap  = new HashMap<>();
                if (CollectionUtils.isNotEmpty(ownerIds)) {
                    List<UserResponse> userResponses = extUserMapper.getUserDetailList(ownerIds);
                    userResponseMap = userResponses.stream().collect(Collectors.toMap(UserResponse::getId, userResponse -> userResponse));
                }
               
                //查商机
                Map<String,List<OpportunityRepeatResponse>> opportunityRepeatResponseMap = new HashMap<>();
                if (permissions.indexOf(PermissionConstants.OPPORTUNITY_MANAGEMENT_READ) > 0 || StringUtils.equalsIgnoreCase(userId, InternalUser.ADMIN.getValue())) {
                
                    if (CollectionUtils.isNotEmpty(customerRepeatResponses)) {
                        List<String> customerIds = customerRepeatResponses.stream().map(CustomerRepeatResponse::getId).collect(Collectors.toList());
                        List<OpportunityRepeatResponse> repeatList = extOpportunityMapper.getRepeatList(customerIds);
                        List<String> flattenedProductIds = repeatList.stream()
                                .flatMap(or -> or.getProducts().stream())
                                .distinct()
                                .toList();
                        // 优化产品名称映射获取
                        Map<String, String> productNameMap = flattenedProductIds.isEmpty() ?
                                Collections.emptyMap() :
                                extProductMapper.listByIds(flattenedProductIds).stream()
                                        .collect(Collectors.toMap(Product::getId, Product::getName));

                        for (OpportunityRepeatResponse opportunityRepeatResponse : repeatList) {
                            List<String> productName = new ArrayList<>();
                            for (String product : opportunityRepeatResponse.getProducts()) {
                                if (productNameMap.get(product)!=null) {
                                    productName.add(productNameMap.get(product));
                                }
                            }
                            opportunityRepeatResponse.setProductNames(productName);
                            opportunityRepeatResponseMap.computeIfAbsent(opportunityRepeatResponse.getCustomerName(), k -> new ArrayList<>());
                            opportunityRepeatResponseMap.get(opportunityRepeatResponse.getCustomerName()).add(opportunityRepeatResponse);
                        }
                    }
                }
                Map<String, UserResponse> finalUserResponseMap = userResponseMap;
                customers.forEach(customer -> {
                    CustomerRepeatResponse customerRepeatResponse = new CustomerRepeatResponse();
                    UserResponse userDetail = finalUserResponseMap.get(customer.getOwner());
                    customerRepeatResponse.setOwnerName(userDetail.getUserName());
                    customerRepeatResponse.setOwnerDepartmentId(userDetail.getDepartmentId());
                    customerRepeatResponse.setOwnerDepartmentName(userDetail.getDepartmentName());
                    BeanUtils.copyBean(customerRepeatResponse, customer);
                    customerRepeatResponse.setOpportunityList(opportunityRepeatResponseMap.get(customer.getName()));    
                    customerRepeatResponses.add(customerRepeatResponse);
                });
            }
        }
        //查线索
        if (permissions.indexOf(PermissionConstants.CLUE_MANAGEMENT_READ) > 0 || StringUtils.equalsIgnoreCase(userId, InternalUser.ADMIN.getValue())) {
            CluePageRequest request = new CluePageRequest();
            request.setKeyword(name);
            request.setSearchType(BusinessSearchType.ALL.name());
            DeptDataPermissionDTO deptDataPermission = new DeptDataPermissionDTO();
            deptDataPermission.setAll(true);
            List<ClueListResponse> list = extClueMapper.list(request, organizationId, userId, deptDataPermission);
            repeatCustomerResponse.setClueList(list);
            if(CollectionUtils.isNotEmpty(customerRepeatResponses)){
                customerRepeatResponses.forEach(customerRepeatResponse -> {
                    List<ClueListResponse> clueListResponses = list.stream().filter(clueListResponse -> StringUtils.equalsIgnoreCase(clueListResponse.getName(), customerRepeatResponse.getName())).toList();
                    customerRepeatResponse.setClueList(clueListResponses);
                });
            }
        }
        
        repeatCustomerResponse.setCustomerData(customerRepeatResponses);
        return repeatCustomerResponse;
    }
}
