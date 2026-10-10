package cn.cordys.crm.customer.mapper;

import cn.cordys.common.dto.*;
import cn.cordys.common.dto.chart.ChartResult;
import cn.cordys.common.statistic.StatisticSqlMapper;
import cn.cordys.crm.customer.domain.CustomerContact;
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
public interface ExtCustomerContactMapper extends StatisticSqlMapper {

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
     *
     * @param customerId 客户ID
     * @param newOwner   新负责人
     * @param oldOwner   旧负责人
     * @param orgId      组织ID
     */
    void updatePoolContactOwner(@Param("customerId") String customerId, @Param("newOwner") String newOwner, @Param("oldOwner") String oldOwner, @Param("orgId") String orgId);

    void updateContactById(@Param("id") String id, @Param("owner") String owner);

    /**
     * 查询客户下挂在指定负责人名下的联系人ID
     * <p>
     * 客户负责人变更时, 这些联系人会整批改派给新负责人; 编辑前把它们记进快照,
     * 审批驳回/撤回时才能按 id 精确还原。
     *
     * @param customerId 客户ID
     * @param owner      负责人
     * @param orgId      组织ID
     *
     * @return 联系人ID集合
     */
    List<String> listOwnedContactIds(@Param("customerId") String customerId, @Param("owner") String owner, @Param("orgId") String orgId);

    /**
     * 按 id 精确还原联系人负责人
     *
     * @param customerId 客户ID
     * @param contactIds 联系人ID集合
     * @param newOwner   还原后的负责人
     * @param orgId      组织ID
     */
    void updateContactOwnerByIds(@Param("customerId") String customerId, @Param("contactIds") List<String> contactIds, @Param("newOwner") String newOwner, @Param("orgId") String orgId);

    List<CustomerContactListResponse> getListByIds(@Param("ids") List<String> ids);

    List<OptionDTO> getContactOptions(@Param("keyword") String keyword, @Param("orgId") String orgId);

    List<GlobalCustomerContactResponse> globalSearchList(@Param("request") BasePageRequest request, @Param("orgId") String orgId);

    long globalSearchListCount(@Param("request") BasePageRequest request, @Param("orgId") String orgId);

    void batchUpdate(@Param("request") BatchUpdateDbParam request);

    /**
     * 批量合并客户联系人
     *
     * @param request 请求参数
     * @param userId  用户ID
     * @param orgId   组织ID
     */
    void batchMerge(@Param("request") CustomerMergeRequest request, @Param("userId") String userId, @Param("orgId") String orgId,
                    @Param("names") List<String> names, @Param("phones") List<String> phones);

    /**
     * 获取待合并的客户联系人列表
     *
     * @param request 请求参数
     * @param orgId   组织ID
     *
     * @return 客户联系人列表
     */
    List<CustomerContact> getMergeContactList(@Param("request") CustomerMergeRequest request, @Param("orgId") String orgId);

    List<ChartResult> chart(@Param("request") ChartAnalysisDbRequest request, @Param("userId") String userId, @Param("orgId") String orgId,
                            @Param("dataPermission") DeptDataPermissionDTO dataPermission);

    void updateCustomerContact(@Param("customerContact")CustomerContact customerContact);
}
