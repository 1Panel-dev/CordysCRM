package io.cordys.crm.customer.mapper;

import io.cordys.crm.customer.dto.request.CustomerBatchTransferRequest;
import org.apache.ibatis.annotations.Param;

/**
 *
 * @author jianxing
 * @date 2025-02-24 11:06:10
 */
public interface ExtCustomerOwnerMapper {

    void batchAdd(@Param("request") CustomerBatchTransferRequest transferRequest, @Param("userId") String userId);
}
