package cn.cordys.crm.system.dto.request;

/**
 * 详情页标签查询请求的公共定位字段。
 * <p>
 * 分页请求与统计请求都通过 {@code relatedFormId}/{@code relatedFieldId} 定位标签，
 * 服务端只依据已保存表单配置解析标签，客户端传入值仅用于匹配。
 * </p>
 */
public interface DetailTabQueryRequest {

    String getRelatedFormId();

    void setRelatedFormId(String relatedFormId);

    String getRelatedFieldId();

    void setRelatedFieldId(String relatedFieldId);
}
