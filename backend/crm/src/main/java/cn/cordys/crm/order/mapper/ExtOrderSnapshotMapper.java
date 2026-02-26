package cn.cordys.crm.order.mapper;

import cn.cordys.crm.order.domain.OrderSnapshot;
import org.apache.ibatis.annotations.Param;

public interface ExtOrderSnapshotMapper {

    void update(@Param("snapshot") OrderSnapshot snapshot);
}
