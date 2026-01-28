package cn.cordys.crm.customer.mapper;

import cn.cordys.crm.customer.domain.CustomerCapacity;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface ExtCustomerCapacityMapper {

  /**
   * 获取容量
   *
   * @param scopeIds 范围ID集合
   * @param organizationId 组织ID
   * @return 容量
   */
  CustomerCapacity getCapacityByScopeIds(
      @Param("ids") List<String> scopeIds, @Param("orgId") String organizationId);

  /**
   * 更新容量
   *
   * @param capacity 容量
   */
  void updateCapacity(@Param("capacity") CustomerCapacity capacity);
}
