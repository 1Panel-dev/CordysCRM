package cn.cordys.crm.customer.mapper;

import cn.cordys.common.dto.*;
import cn.cordys.common.dto.chart.ChartResult;
import cn.cordys.crm.customer.dto.request.ContactUniqueRequest;
import cn.cordys.crm.customer.dto.request.CustomerContactPageRequest;
import cn.cordys.crm.customer.dto.request.CustomerMergeRequest;
import cn.cordys.crm.customer.dto.response.CustomerContactListResponse;
import cn.cordys.crm.home.dto.request.HomeStatisticSearchWrapperRequest;
import cn.cordys.crm.search.response.advanced.AdvancedCustomerContactResponse;
import cn.cordys.crm.search.response.global.GlobalCustomerContactResponse;
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

    List<AdvancedCustomerContactResponse> getSimilarContactList(@Param("request") CustomerContactPageRequest request, @Param("userId") String userId, @Param("orgId") String organizationId);

    /**
     * 获取联系人数量(唯一性校验)
     *
     * @param uniqueRequest 请求参数
     * @param customerId    客户ID
     * @param orgId         组织ID
     *
     * @return 联系人数量
     */
    long getUniqueContactCount(@Param("request") ContactUniqueRequest uniqueRequest, @Param("customerId") String customerId, @Param("orgId") String orgId);

    Long getNewContactCount(@Param("request") HomeStatisticSearchWrapperRequest request);

    void updateContactOwner(@Param("customerId") String customerId, @Param("newOwner") String newOwner, @Param("oldOwner") String oldOwner, @Param("orgId") String orgId);

    /**
     * 更新公海联系人负责人
     * @param customerId 客户ID
     * @param newOwner 新负责人
     * @param oldOwner 旧负责人
     * @param orgId 组织ID
     */
    void updatePoolContactOwner(@Param("customerId") String customerId, @Param("newOwner") String newOwner, @Param("oldOwner") String oldOwner, @Param("orgId") String orgId);

    void updateContactById(@Param("id") String id, @Param("owner") String owner);

    List<CustomerContactListResponse> getListByIds(@Param("ids") List<String> ids);

    List<OptionDTO> getContactOptions(@Param("keyword") String keyword, @Param("orgId") String orgId);

    List<GlobalCustomerContactResponse> globalSearchList(@Param("request") BasePageRequest request, @Param("orgId") String orgId);

    long globalSearchListCount(@Param("request") BasePageRequest request, @Param("orgId") String orgId);

    void batchUpdate(@Param("request") BatchUpdateDbParam request);

    /**
     * 批量合并客户联系人
     * @param request 请求参数
     * @param userId 用户ID
     * @param orgId 组织ID
     */
    void batchMerge(@Param("request") CustomerMergeRequest request, @Param("userId") String userId, @Param("orgId") String orgId);

    List<ChartResult> chart(@Param("request") ChartAnalysisDbRequest request, @Param("userId") String userId, @Param("orgId") String orgId,
                      @Param("dataPermission") DeptDataPermissionDTO dataPermission);
}
