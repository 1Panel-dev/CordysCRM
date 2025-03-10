package io.cordys.crm.clue.mapper;

import io.cordys.common.dto.DeptDataPermissionDTO;
import io.cordys.common.dto.OptionDTO;
import io.cordys.crm.clue.domain.Clue;
import io.cordys.crm.clue.dto.request.ClueBatchTransferRequest;
import io.cordys.crm.clue.dto.request.CluePageRequest;
import io.cordys.crm.clue.dto.response.ClueListResponse;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 *
 * @author jianxing
 * @date 2025-02-08 17:42:41
 */
public interface ExtClueMapper {

    List<ClueListResponse> list(@Param("request") CluePageRequest request, @Param("orgId") String orgId,
                                    @Param("userId") String userId, @Param("dataPermission") DeptDataPermissionDTO deptDataPermission);

    boolean checkAddExist(@Param("clue") Clue clue);

    boolean checkUpdateExist(@Param("clue") Clue Clue);

    List<OptionDTO> selectOptionByIds(@Param("ids") List<String> ids);

    void batchTransfer(@Param("request") ClueBatchTransferRequest request);
}
