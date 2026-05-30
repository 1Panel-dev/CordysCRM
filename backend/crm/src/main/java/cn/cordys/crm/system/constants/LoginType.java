package cn.cordys.crm.system.constants;

import lombok.Getter;

@Getter
public enum LoginType {
    /**
     * 电脑端
     */
    WEB("WEB"),
    /**
     * 手机端
     */
    MOBILE("MOBILE");

    private final String name;

    LoginType(String name) {
        this.name = name;
    }

}
