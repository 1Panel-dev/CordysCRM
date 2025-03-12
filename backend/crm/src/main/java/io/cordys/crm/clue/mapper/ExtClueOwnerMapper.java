package io.cordys.crm.clue.mapper;

import io.cordys.crm.clue.dto.request.ClueBatchTransferRequest;
import org.apache.ibatis.annotations.Param;

/**
 *
 * @author jianxing
 * @date 2025-02-24 11:06:10
 */
public interface ExtClueOwnerMapper {

    void batchAdd(@Param("request") ClueBatchTransferRequest transferRequest, @Param("userId") String userId);
}
