package cn.cordys.crm.search.constants;

import java.util.List;

public class SearchPhoneEnum {

    public static final String SEARCH_CN = "(+86)"; // 大陆
    public static final String SEARCH_TW = "(+886)"; // 台湾
    public static final String SEARCH_HK = "(+852)"; // 香港
    public static final String SEARCH_MO = "(+853)"; // 澳门



    public static final List<String> VALUES = List.of(
            SEARCH_CN,
            SEARCH_TW,
            SEARCH_HK,
            SEARCH_MO
    );
}
