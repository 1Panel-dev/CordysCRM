package cn.cordys.crm.clue.mapper;

import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.dto.BasePageRequest;
import cn.cordys.common.dto.BatchUpdateDbParam;
import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.crm.clue.domain.Clue;
import cn.cordys.crm.clue.dto.request.ClueBatchTransferRequest;
import cn.cordys.crm.clue.dto.request.CluePageRequest;
import cn.cordys.crm.clue.dto.response.ClueListResponse;
import cn.cordys.crm.home.dto.request.HomeStatisticSearchWrapperRequest;
import cn.cordys.crm.search.response.advanced.AdvancedCluePoolResponse;
import cn.cordys.crm.search.response.advanced.AdvancedClueResponse;
import cn.cordys.crm.search.response.global.GlobalCluePoolResponse;
import cn.cordys.crm.search.response.global.GlobalClueResponse;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
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
     *
     * @param clue 线索池
     */
    void moveToPool(@Param("clue") Clue clue);

    /**
     * 获取线索重复数据数量
     *
     * @param customerNames 客户名称列表
     *
     * @return 重复数据数量
     */
    List<OptionDTO> getRepeatCountMap(@Param("customerNames") List<String> customerNames);

    /**
     * 获取相似线索列表
     *
     * @param customerName 客户名称
     *
     * @return 相似线索列表
     */
    List<AdvancedClueResponse> getSimilarClueList(@Param("customerName") String customerName, @Param("orgId") String orgId);

    /**
     * 获取重复线索列表
     *
     * @param customerName 客户名称
     *
     * @return 重复线索列表
     */
    List<AdvancedClueResponse> getRepeatClueList(@Param("customerName") String customerName, @Param("orgId") String orgId);

    /**
     * 查询用户负责的线索条数
     *
     * @param ownerId 负责用户ID
     *
     * @return 数量
     */
    long getOwnerCount(@Param("ownerId") String ownerId);

    List<ClueListResponse> getListByIds(@Param("ids") List<String> ids);

    Long selectClueCount(@Param("request") HomeStatisticSearchWrapperRequest request, @Param("unfollowed") boolean unfollowed);

    List<AdvancedCluePoolResponse> cluePoolList(@Param("request") BasePageRequest request, @Param("orgId") String orgId);

    List<OptionDTO> getClueOptions(@Param("keyword") String keyword, @Param("orgId") String orgId);

    List<GlobalClueResponse> globalSearchList(@Param("request") BasePageRequest request, @Param("orgId") String orgId);

    long globalSearchListCount(@Param("request") BasePageRequest request, @Param("orgId") String orgId);


    List<GlobalCluePoolResponse> globalPoolSearchList(@Param("request") BasePageRequest request, @Param("orgId") String orgId);

    long globalPoolSearchListCount(@Param("request") BasePageRequest request, @Param("orgId") String orgId);

    List<Clue> searchColumnsByIds(@Param("columns") List<String> columns, @Param("ids") List<String> ids);


    /**
     * 根据ID全量更新线索
     *
     * @param clue 线索
     */
    void updateIncludeNullById(@Param("clue") Clue clue);

    /**
     * 检查字段值是否唯一
     *
     * @param field 字段值
     *
     * @return bool
     */
    boolean checkFieldValueRepeat(@Param("field") BaseModuleFieldValue field);

    void batchUpdate(@Param("request")BatchUpdateDbParam request);
}
