package io.cordys.crm.customer.mapper;

import io.cordys.common.dto.DeptDataPermissionDTO;
import io.cordys.common.dto.OptionDTO;
import io.cordys.crm.customer.dto.request.ContactUniqueRequest;
import io.cordys.crm.customer.dto.request.CustomerContactPageRequest;
import io.cordys.crm.customer.dto.response.CustomerContactListResponse;
import io.cordys.crm.home.dto.request.HomeStatisticSearchWrapperRequest;
import io.cordys.crm.search.response.advanced.AdvancedCustomerContactResponse;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author jianxing
 * @date 2025-02-24 11:06:10
 */
public interface ExtCustomerContactMapper {

    List<CustomerContactListResponse> list(@Param("request") CustomerContactPageRequest request, @Param("userId") String userId, @Param("orgId") String orgId,
                                           @Param("dataPermission") DeptDataPermissionDTO dataPermission);

    List<OptionDTO> selectContactOptionByIds(List<String> contactIds);

    List<CustomerContactListResponse> listByCustomerId(@Param("customerId") String customerId);

    List<CustomerContactListResponse> getById(@Param("id") String id);

    List<OptionDTO> selectContactPhoneOptionByIds(@Param("contactIds") List<String> contactIds);

    List<AdvancedCustomerContactResponse> getSimilarContactList(@Param("request") CustomerContactPageRequest request, @Param("userId") String userId, @Param("orgId")String organizationId);

    /**
     * 获取联系人数量(唯一性校验)
     * @param uniqueRequest 请求参数
     * @param customerId 客户ID
     * @param orgId 组织ID
     * @return 联系人数量
     */
    long getUniqueContactCount(@Param("request") ContactUniqueRequest uniqueRequest, @Param("customerId") String customerId, @Param("orgId") String orgId);

    Long getNewContactCount(@Param("request") HomeStatisticSearchWrapperRequest request);

    void updateContactOwner(@Param("customerId") String customerId, @Param("newOwner") String newOwner, @Param("oldOwner") String oldOwner, @Param("orgId") String orgId);

    void updateContactById(@Param("id") String id, @Param("owner") String owner);

    List<CustomerContactListResponse> getListByIds(@Param("ids")List<String> ids);

    List<OptionDTO> getContactOptions(@Param("keyword") String keyword, @Param("orgId") String orgId);

}
