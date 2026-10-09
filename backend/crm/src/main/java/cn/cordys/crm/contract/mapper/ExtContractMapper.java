package cn.cordys.crm.contract.mapper;

import cn.cordys.common.dto.BatchUpdateDbParam;
import cn.cordys.common.dto.DeptDataPermissionDTO;
import cn.cordys.common.dto.condition.BaseCondition;
import cn.cordys.common.statistic.StatisticSqlMapper;
import cn.cordys.crm.contract.domain.Contract;
import cn.cordys.crm.contract.dto.request.ContractPageRequest;
import cn.cordys.crm.contract.dto.response.ContractGetResponse;
import cn.cordys.crm.contract.dto.response.ContractListResponse;
import cn.cordys.crm.contract.dto.response.ContractStatisticResponse;
import cn.cordys.crm.contract.dto.response.CustomerContractStatisticResponse;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtContractMapper extends StatisticSqlMapper {


    List<ContractListResponse> list(@Param("request") ContractPageRequest request, @Param("orgId") String orgId,
                                    @Param("userId") String userId, @Param("dataPermission") DeptDataPermissionDTO deptDataPermission, @Param("source") boolean source);

    ContractGetResponse getDetail(@Param("id") String id);

    List<ContractListResponse> getListByIds(@Param("ids") List<String> ids, @Param("userId") String userId, @Param("orgId") String orgId, @Param("dataPermission") DeptDataPermissionDTO deptDataPermission);

    CustomerContractStatisticResponse calculateContractStatisticByCustomerId(@Param("customerId") String customerId, @Param("userId") String userId, @Param("orgId") String orgId, @Param("dataPermission") DeptDataPermissionDTO deptDataPermission);

    List<String> selectByStatusAndIds(@Param("ids") List<String> ids, @Param("approvalStatus") String approvalStatus);

    void updateStatus(@Param("id") String id, @Param("approvalStatus") String approvalStatus, @Param("userId") String userId, @Param("updateTime") long updateTime);

    void updateStage(@Param("id") String id, @Param("stage") String stage, @Param("userId") String userId, @Param("updateTime") long updateTime);

    /**
     * 审批驳回/撤回时回退阶段
     *
     * @param id         合同ID
     * @param stage      回退到的阶段
     * @param voidReason 作废原因, 回退到非作废阶段时为 null
     * @param userId     用户ID
     * @param updateTime 更新时间
     */
    void revertStageByApproval(@Param("id") String id, @Param("stage") String stage, @Param("voidReason") String voidReason,
                               @Param("userId") String userId, @Param("updateTime") long updateTime);

    List<Contract> selectByTimestamp(@Param("organizationId") String organizationId, @Param("timestampOld") long timestampOld, @Param("timestamp") long timestamp);

    void batchUpdate(@Param("request") BatchUpdateDbParam request);

    ContractStatisticResponse searchStatistic(@Param("request") BaseCondition request, @Param("orgId") String orgId, @Param("userId") String userId, @Param("dataPermission") DeptDataPermissionDTO dataPermission);

    int countByStage(@Param("stage") String stage);

    Long selectNextPos(@Param("orgId") String orgId, @Param("stage") String stage);

    void moveUpStageContract(@Param("end") Long end, @Param("stage") String stage, @Param("pos") Long pos);

    void moveDownStageContract(@Param("end") Long end, @Param("stage") String stage, @Param("pos") Long pos);

    void updateOldApprovalStatusNone();

    void updateContract(@Param("contract") Contract contract);
}
