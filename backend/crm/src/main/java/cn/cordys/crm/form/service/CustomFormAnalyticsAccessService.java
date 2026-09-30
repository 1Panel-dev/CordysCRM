package cn.cordys.crm.form.service;

import cn.cordys.crm.form.domain.CustomFormRoleKey;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/** 新增统计入口复用表单列表的授权判断，不改变既有数据接口。 */
@Service
public class CustomFormAnalyticsAccessService {
    @Resource
    private CustomFormDataService customFormDataService;

    public boolean manageOwn(String formId, String userId, String orgId) {
        return customFormDataService.getDataScope(formId, userId, orgId) == CustomFormRoleKey.MANAGE_OWN;
    }
}
