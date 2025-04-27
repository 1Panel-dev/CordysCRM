package io.cordys.crm.system.constants;

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

    public String getName() {
        return name;
    }
}
