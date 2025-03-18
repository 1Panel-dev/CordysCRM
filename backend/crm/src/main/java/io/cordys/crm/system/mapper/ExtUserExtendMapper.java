package io.cordys.crm.system.mapper;


import io.cordys.crm.system.dto.ScopeNameDTO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtUserExtendMapper {
    void deleteUser(@Param("ids") List<String> ids);

    /**
     * 根据范围ID分组
     * @param scopeIds 范围ID集合
     * @return 分组结果
     */
    List<ScopeNameDTO> groupByScopeIds(@Param("ids") List<String> scopeIds);
}
