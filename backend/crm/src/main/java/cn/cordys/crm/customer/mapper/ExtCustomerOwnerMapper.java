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
     * 删除转移写入的负责人变更记录
     * <p>
     * 只匹配同时满足转移前负责人与领取时间的最新一条, 避免误删同一负责人历史上更早的记录。
     *
     * @param customerId     客户ID
     * @param owner          转移前的负责人
     * @param collectionTime 转移前的领取时间
     */
    void deleteTransferHistory(@Param("customerId") String customerId, @Param("owner") String owner, @Param("collectionTime") Long collectionTime);

    /**
     * 获取最近的客户负责人
     *
     * @param customerId 客户ID
     *
     * @return 负责人ID
     */
    String getRecentOwner(@Param("customerId") String customerId);
}
