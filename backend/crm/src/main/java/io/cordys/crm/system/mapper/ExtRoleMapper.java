package io.cordys.crm.system.mapper;

import io.cordys.crm.system.domain.Role;
import org.apache.ibatis.annotations.Param;

/**
 *
 * @author jianxing
 * @date 2025-01-10 18:35:02
 */
public interface ExtRoleMapper {

    boolean checkAddExist(@Param("role") Role role);
    boolean checkUpdateExist(@Param("role") Role role);
}
