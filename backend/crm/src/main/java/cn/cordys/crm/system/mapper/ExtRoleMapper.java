package cn.cordys.crm.system.mapper;

import cn.cordys.common.dto.OptionDTO;
import cn.cordys.crm.system.domain.Role;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author jianxing
 * @date 2025-01-10 18:35:02
 */
public interface ExtRoleMapper {

    boolean checkAddExist(@Param("role") Role role);

    boolean checkUpdateExist(@Param("role") Role role);

    List<OptionDTO> getIdNameByIds(@Param("ids") List<String> ids);
}
