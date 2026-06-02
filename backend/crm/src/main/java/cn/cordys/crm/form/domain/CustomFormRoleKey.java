package cn.cordys.crm.form.domain;

import cn.cordys.common.util.Translator;
import lombok.Getter;

@Getter
public enum CustomFormRoleKey {

    MANAGE_ALL("manage_all", "custom.form.role.manage_all"),
    VIEW_ALL("view_all", "custom.form.role.view_all"),
    MANAGE_OWN("manage_own", "custom.form.role.manage_own");

    private final String key;
    private final String i18nKey;

    CustomFormRoleKey(String key, String i18nKey) {
        this.key = key;
        this.i18nKey = i18nKey;
    }

    public String getName() {
        return Translator.get(i18nKey);
    }
}
