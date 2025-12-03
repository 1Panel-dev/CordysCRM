package cn.cordys.crm.contract.mapper;

import cn.cordys.crm.contract.domain.ContractSnapshot;
import org.apache.ibatis.annotations.Param;

public interface ExtContractSnapshotMapper {

    void update(@Param("snapshot") ContractSnapshot snapshot);
}
