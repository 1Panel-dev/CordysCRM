package io.cordys.crm.customer.mapper;

import io.cordys.common.dto.BasePageRequest;
import io.cordys.common.dto.DeptDataPermissionDTO;
import io.cordys.common.dto.OptionDTO;
import io.cordys.crm.customer.domain.Customer;
import io.cordys.crm.customer.dto.request.CustomerBatchTransferRequest;
import io.cordys.crm.customer.dto.request.CustomerPageRequest;
import io.cordys.crm.customer.dto.response.CustomerListResponse;
import io.cordys.crm.home.dto.request.HomeStatisticSearchWrapperRequest;
import io.cordys.crm.search.response.advanced.AdvancedCustomerPoolResponse;
import io.cordys.crm.search.response.advanced.AdvancedCustomerResponse;
import io.cordys.crm.search.response.global.GlobalCustomerPoolResponse;
import io.cordys.crm.system.dto.FilterConditionDTO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author jianxing
 * @date 2025-02-08 17:42:41
 */
public interface ExtCustomerMapper {

    List<CustomerListResponse> list(@Param("request") CustomerPageRequest request, @Param("orgId") String orgId,
                                    @Param("userId") String userId, @Param("dataPermission") DeptDataPermissionDTO deptDataPermission);

    List<CustomerListResponse> sourceList(@Param("request") CustomerPageRequest request, @Param("orgId") String orgId,
                                          @Param("userId") String userId, @Param("dataPermission") DeptDataPermissionDTO deptDataPermission);

    List<OptionDTO> selectOptionByIds(@Param("ids") List<String> ids);

    void batchTransfer(@Param("request") CustomerBatchTransferRequest request, @Param("userId") String userId);

    List<AdvancedCustomerResponse> checkRepeatCustomer(@Param("request") CustomerPageRequest request, @Param("orgId") String orgId,
                                                       @Param("userId") String userId);

    int countByOwner(@Param("owner") String owner);

    /**
     * 移入公海
     *
     * @param customer 客户
     */
    void moveToPool(@Param("customer") Customer customer);

    List<OptionDTO> getCustomerOptions(@Param("keyword") String keyword, @Param("orgId") String orgId);

    List<OptionDTO> getCustomerOptionsByIds(@Param("ids") List<String> ids);

    boolean hasRefOpportunity(@Param("ids") List<String> ids);

    /**
     * 查询负责人过滤条件下的客户数量
     *
     * @param ownerId 负责人ID
     * @param filters 过滤条件集合
     * @return 客户数量
     */
    long filterOwnerCount(@Param("ownerId") String ownerId, @Param("filters") List<FilterConditionDTO> filters);

    List<CustomerListResponse> getListByIds(@Param("ids") List<String> ids);

    Long selectCustomerCount(@Param("request") HomeStatisticSearchWrapperRequest request, @Param("unfollowed") boolean unfollowed);

    List<AdvancedCustomerPoolResponse> customerPoolList(@Param("request") BasePageRequest request, @Param("orgId") String orgId);

    List<GlobalCustomerPoolResponse> globalPoolSearchList(@Param("request") BasePageRequest request, @Param("orgId") String orgId);
}
