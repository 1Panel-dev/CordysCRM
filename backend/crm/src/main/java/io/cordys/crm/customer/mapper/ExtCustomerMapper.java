package io.cordys.crm.customer.mapper;

import io.cordys.common.dto.DeptDataPermissionDTO;
import io.cordys.common.dto.OptionDTO;
import io.cordys.crm.customer.dto.request.*;
import io.cordys.crm.customer.dto.response.*;
import org.apache.ibatis.annotations.Param;
import io.cordys.crm.customer.domain.Customer;

import java.util.List;

/**
 *
 * @author jianxing
 * @date 2025-02-08 17:42:41
 */
public interface ExtCustomerMapper {

    List<CustomerListResponse> list(@Param("request") CustomerPageRequest request, @Param("orgId") String orgId,
                                    @Param("userId") String userId, @Param("dataPermission") DeptDataPermissionDTO deptDataPermission);

    List<CustomerListResponse> sourceList(@Param("request") CustomerPageRequest request, @Param("orgId") String orgId, @Param("userId") String userId);

    boolean checkAddExist(@Param("customer") Customer customer);

    boolean checkUpdateExist(@Param("customer") Customer Customer);

    List<OptionDTO> selectOptionByIds(@Param("ids") List<String> ids);

    void batchTransfer(@Param("request") CustomerBatchTransferRequest request, @Param("userId") String userId);

    List<CustomerRepeatResponse> checkRepeatCustomerByName(@Param("name") String name, @Param("orgId") String orgId);

    int countByOwner(@Param("owner") String owner);

    /**
     * 移入公海
     * @param customer 客户
     */
    void moveToPool(@Param("customer") Customer customer);

    List<OptionDTO> getCustomerOptions(@Param("keyword") String keyword, @Param("orgId") String orgId);

    List<OptionDTO> getCustomerOptionsByIds(@Param("ids") List<String> ids);

    boolean hasRefOpportunity(@Param("ids") List<String> ids);

}
