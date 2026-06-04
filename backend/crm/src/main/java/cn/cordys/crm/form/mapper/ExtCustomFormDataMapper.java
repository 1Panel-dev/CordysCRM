package cn.cordys.crm.form.mapper;

import cn.cordys.common.dto.BatchUpdateDbParam;
import cn.cordys.crm.form.dto.request.CustomFormDataPageRequest;
import cn.cordys.crm.form.dto.response.CustomFormDataListResponse;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtCustomFormDataMapper {

    List<CustomFormDataListResponse> list(@Param("request") CustomFormDataPageRequest request,
                                          @Param("orgId") String orgId,
                                          @Param("userId") String userId,
                                          @Param("manageOwn") boolean manageOwn);

    void batchUpdate(@Param("request") BatchUpdateDbParam batchUpdateDbParam);

    void deleteFormDataByCustomFormId(@Param("formId") String formId);
}
