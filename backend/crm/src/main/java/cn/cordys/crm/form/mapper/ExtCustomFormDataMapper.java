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

    List<CustomFormDataListResponse> listForExport(@Param("request") CustomFormDataPageRequest request,
                                                   @Param("orgId") String orgId,
                                                   @Param("userId") String userId,
                                                   @Param("manageOwn") boolean manageOwn,
                                                   @Param("limit") int limit,
                                                   @Param("offset") int offset);

    void batchUpdate(@Param("request") BatchUpdateDbParam batchUpdateDbParam);

    void deleteFormDataByCustomFormId(@Param("formId") String formId);

    List<CustomFormDataListResponse> getListByIds(@Param("ids") List<String> ids);
}
