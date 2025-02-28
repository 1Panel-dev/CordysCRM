package io.cordys.crm.customer.mapper;

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

    List<CustomerListResponse> list(@Param("request") CustomerPageRequest request, @Param("orgId") String orgId);

    boolean checkAddExist(@Param("customer") Customer customer);

    boolean checkUpdateExist(@Param("customer") Customer Customer);

    List<OptionDTO> selectOptionByIds(@Param("ids") List<String> ids);
}
