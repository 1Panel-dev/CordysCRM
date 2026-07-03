package cn.cordys.crm.approval.mapper;

import cn.cordys.crm.approval.domain.ApprovalResourceSnapshot;
import org.apache.ibatis.annotations.Param;

public interface ExtApprovalResourceSnapshotMapper {
    ApprovalResourceSnapshot selectByResourceId(@Param("resourceId") String resourceId);

    void deleteByResourceId(@Param("resourceId") String resourceId);
}
