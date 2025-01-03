package io.cordys.crm.system.mapper;

import io.cordys.crm.system.dto.request.*;
import io.cordys.crm.system.dto.response.*;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 *
 * @author jianxing
 * @date 2025-01-03 11:53:07
 */
public interface ExtRoleMapper {

    List<RoleListResponse> list(@Param("request") RolePageRequest request);
}
