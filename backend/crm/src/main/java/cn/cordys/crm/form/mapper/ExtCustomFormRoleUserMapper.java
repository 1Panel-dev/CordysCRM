package cn.cordys.crm.form.mapper;

import cn.cordys.common.dto.BasePageRequest;
import cn.cordys.crm.form.dto.response.CustomFormRoleUserListResponse;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtCustomFormRoleUserMapper {

    List<CustomFormRoleUserListResponse> listByRoleId(@Param("roleId") String roleId,
                                                       @Param("orgId") String orgId,
                                                       @Param("request") BasePageRequest request);
}
