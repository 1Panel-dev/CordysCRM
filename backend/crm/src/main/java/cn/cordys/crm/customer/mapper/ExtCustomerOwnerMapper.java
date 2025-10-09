package cn.cordys.crm.customer.mapper;

import cn.cordys.crm.customer.dto.request.CustomerBatchTransferRequest;
import org.apache.ibatis.annotations.Param;

/**
 * @author jianxing
 * @date 2025-02-24 11:06:10
 */
public interface ExtCustomerOwnerMapper {

    void batchAdd(@Param("request") CustomerBatchTransferRequest transferRequest, @Param("userId") String userId);

    /**
     * 获取最近的客户负责人
     * @param customerId 客户ID
     * @return 负责人ID
     */
    String getRecentOwner(@Param("customerId") String customerId);
}
