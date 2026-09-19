package cn.cordys.crm.system.service.detailtab;

import cn.cordys.common.pager.PagerWithOption;

/**
 * 详情页标签查询扩展点。
 *
 * <p>Handler 必须复用对应领域 Service，并在查询前完成目标模块权限及数据范围校验。
 * 父资源权限由 Controller 的资源级权限注解统一保证。</p>
 */
public interface DetailTabDataHandler {

    boolean supports(DetailTabQueryContext context);

    PagerWithOption<?> page(DetailTabQueryContext context);
}
