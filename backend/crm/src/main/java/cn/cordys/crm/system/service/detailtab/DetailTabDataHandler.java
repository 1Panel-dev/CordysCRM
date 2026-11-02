package cn.cordys.crm.system.service.detailtab;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.common.util.Translator;

/**
 * 详情页标签查询扩展点。
 * <p>
 * Handler 必须复用对应领域 Service，并在查询前完成目标模块权限及数据范围校验。
 * 父资源权限由 Controller 的资源级权限注解统一保证。
 * </p>
 */
public interface DetailTabDataHandler {

    boolean supports(DetailTabQueryContext context);

    PagerWithOption<?> page(DetailTabQueryContext context);

    /**
     * 标签金额统计（如商机金额、合同金额），与对应主列表统计口径一致。
     * <p>
     * 默认不支持统计，需要金额统计的关联表单由具体 Handler 覆写。
     * </p>
     */
    default Object statistic(DetailTabQueryContext context) {
        throw new GenericException(Translator.get("module.tab.statistic.not_support"));
    }
}
