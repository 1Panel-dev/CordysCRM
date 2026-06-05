package cn.cordys.crm.form.mapper;

import cn.cordys.crm.form.dto.request.CustomFormRoleUserPageRequest;
import cn.cordys.crm.form.dto.response.CustomFormRoleUserListResponse;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtCustomFormRoleUserMapper {

    List<CustomFormRoleUserListResponse> listByRoleId(@Param("orgId") String orgId,
                                                       @Param("request") CustomFormRoleUserPageRequest request);
}
