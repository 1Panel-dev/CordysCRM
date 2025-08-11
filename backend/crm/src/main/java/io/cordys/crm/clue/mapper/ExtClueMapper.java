package io.cordys.crm.clue.mapper;

import io.cordys.common.dto.BasePageRequest;
import io.cordys.common.dto.DeptDataPermissionDTO;
import io.cordys.common.dto.OptionDTO;
import io.cordys.crm.clue.domain.Clue;
import io.cordys.crm.clue.dto.request.ClueBatchTransferRequest;
import io.cordys.crm.clue.dto.request.CluePageRequest;
import io.cordys.crm.clue.dto.response.ClueListResponse;
import io.cordys.crm.home.dto.request.HomeStatisticSearchWrapperRequest;
import io.cordys.crm.search.response.GlobalCluePoolResponse;
import io.cordys.crm.search.response.GlobalClueResponse;
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

    List<OptionDTO> selectOptionByIds(@Param("ids") List<String> ids);

    void batchTransfer(@Param("request") ClueBatchTransferRequest request);

    int countByOwner(@Param("owner") String owner);

    /**
     * 移入线索池
     * @param clue 线索池
     */
    void moveToPool(@Param("clue")Clue clue);

    /**
     * 获取线索重复数据数量
     * @param customerNames 客户名称列表
     * @return 重复数据数量
     */
    List<OptionDTO> getRepeatCountMap(@Param("customerNames") List<String> customerNames);

    /**
     * 获取相似线索列表
     * @param customerName 客户名称
     * @return 相似线索列表
     */
    List<GlobalClueResponse> getSimilarClueList(@Param("customerName") String customerName, @Param("orgId") String orgId);

    /**
     * 获取重复线索列表
     * @param customerName 客户名称
     * @return 重复线索列表
     */
    List<GlobalClueResponse> getRepeatClueList(@Param("customerName") String customerName, @Param("orgId") String orgId);

    /**
     * 查询用户负责的线索条数
     * @param ownerId 负责用户ID
     * @return 数量
     */
    long getOwnerCount(@Param("ownerId") String ownerId);

    List<ClueListResponse> getListByIds(@Param("ids") List<String> ids);

    Long selectClueCount(@Param("request") HomeStatisticSearchWrapperRequest request, @Param("unfollowed")  boolean unfollowed);

    List<GlobalCluePoolResponse> cluePoolList(@Param("request") BasePageRequest request, @Param("orgId") String orgId);
}
