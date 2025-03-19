package io.cordys.crm.customer.mapper;

import io.cordys.common.dto.DeptDataPermissionDTO;
import io.cordys.common.dto.OptionDTO;
import io.cordys.crm.customer.dto.request.*;
import io.cordys.crm.customer.dto.response.*;
import org.apache.ibatis.annotations.Param;
import io.cordys.crm.customer.domain.CustomerContact;

import java.util.List;

/**
 *
 * @author jianxing
 * @date 2025-02-24 11:06:10
 */
public interface ExtCustomerContactMapper {

    List<CustomerContactListResponse> list(@Param("request") CustomerContactPageRequest request, @Param("orgId") String orgId,
                                           @Param("dataPermission") DeptDataPermissionDTO dataPermission);

    boolean checkAddExist(@Param("customerContact") CustomerContact customerContact);

    boolean checkUpdateExist(@Param("customerContact") CustomerContact CustomerContact);

    List<OptionDTO> selectContactOptionByIds(List<String> contactIds);

    List<CustomerContactListResponse> listByCustomerId(@Param("customerId") String customerId);
}
