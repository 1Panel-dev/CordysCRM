package cn.cordys.crm.system.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.crm.system.dto.form.FormDetailTab;
import cn.cordys.crm.system.dto.form.FormProp;
import cn.cordys.crm.system.dto.request.DetailTabPageRequest;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.service.detailtab.DetailTabDataHandler;
import cn.cordys.crm.system.service.detailtab.DetailTabQueryContext;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 详情页标签数据统一编排服务。
 *
 * <p>客户端传入的关联表单和字段只用于定位标签，真实查询条件始终取自当前组织的
 * 已保存表单配置，防止通过篡改请求读取未配置的关联数据。</p>
 */
@Service
@RequiredArgsConstructor
public class DetailTabDataService {

    private final ModuleFormCacheService moduleFormCacheService;
    private final List<DetailTabDataHandler> handlers;

    public PagerWithOption<?> page(String formKey, String resourceId, DetailTabPageRequest request,
                                   String userId, String organizationId) {
        FormDetailTab tab = resolveEnabledTab(formKey, request, organizationId);
        DetailTabQueryContext context = new DetailTabQueryContext(
                formKey, resourceId, tab, request, userId, organizationId);

        DetailTabDataHandler handler = handlers.stream()
                .filter(item -> item.supports(context))
                .findFirst()
                .orElseThrow(() -> new GenericException("当前标签暂不支持数据查询"));

        return handler.page(context);
    }

    private FormDetailTab resolveEnabledTab(String formKey, DetailTabPageRequest request, String organizationId) {
        ModuleFormConfigDTO config = moduleFormCacheService.getConfig(formKey, organizationId);
        FormProp formProp = config == null ? null : config.getFormProp();
        List<FormDetailTab> tabs = formProp == null ? null : formProp.getDetailTabs();
        if (CollectionUtils.isEmpty(tabs)) {
            throw new GenericException("当前表单未配置详情页标签");
        }

        if (StringUtils.isBlank(request.getRelatedFormId())) {
            throw new GenericException("关联表单不能为空");
        }

        return tabs.stream()
                .filter(tab -> Boolean.TRUE.equals(tab.getEnable()))
                .filter(tab -> matchesTab(tab, request))
                .findFirst()
                .orElseThrow(() -> new GenericException("详情页标签不存在或未启用"));
    }

    private boolean matchesTab(FormDetailTab tab, DetailTabPageRequest request) {
        // 独立内置标签没有关联表单，related 接口会以 internalKey 作为其稳定 ID 返回。
        String relatedFormId = tab.getRelatedForm() == null
                ? tab.getInternalKey() : tab.getRelatedForm().getIdAsString();
        if (!Strings.CS.equals(relatedFormId, request.getRelatedFormId())) {
            return false;
        }

        // 自定义标签允许同一关联表单选择不同字段，必须组合字段 ID 才能唯一定位。
        return tab.getRelatedField() != null
                && StringUtils.isNotBlank(request.getRelatedFieldId())
                && Strings.CS.equals(tab.getRelatedField().getIdAsString(), request.getRelatedFieldId());
    }
}
