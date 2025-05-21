package io.cordys.crm.customer.mapper;

import io.cordys.common.dto.DeptDataPermissionDTO;
import io.cordys.common.dto.OptionDTO;
import io.cordys.crm.customer.domain.CustomerContact;
import io.cordys.crm.customer.dto.request.CustomerContactPageRequest;
import io.cordys.crm.customer.dto.response.CustomerContactListResponse;
import org.apache.ibatis.annotations.Param;

import java.util.Arrays;
import java.util.List;

/**
 * @author jianxing
 * @date 2025-02-24 11:06:10
 */
public interface ExtCustomerContactMapper {

    List<CustomerContactListResponse> list(@Param("request") CustomerContactPageRequest request, @Param("userId") String userId, @Param("orgId") String orgId,
                                           @Param("dataPermission") DeptDataPermissionDTO dataPermission);

    List<CustomerContactListResponse> sourceList(@Param("request") CustomerContactPageRequest request, @Param("orgId") String orgId,
                                                 @Param("userId") String userId);

    boolean checkAddExist(@Param("customerContact") CustomerContact customerContact);

    boolean checkUpdateExist(@Param("customerContact") CustomerContact CustomerContact);

    List<OptionDTO> selectContactOptionByIds(List<String> contactIds);

    List<CustomerContactListResponse> listByCustomerId(@Param("customerId") String customerId);

    List<CustomerContactListResponse> getById(@Param("id") String id);

    List<OptionDTO> selectContactPhoneOptionByIds(@Param("contactIds") List<String> contactIds);
}
