package cn.cordys.crm.system.mapper;

import cn.cordys.common.dto.OptionDTO;
import cn.cordys.crm.system.domain.Role;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * @author jianxing
 * @date 2025-01-10 18:35:02
 */
public interface ExtRoleMapper {

  boolean checkAddExist(@Param("role") Role role);

  boolean checkUpdateExist(@Param("role") Role role);

  List<String> getInternalRoleIds();

  List<OptionDTO> getIdNameByIds(@Param("ids") List<String> ids);
}
