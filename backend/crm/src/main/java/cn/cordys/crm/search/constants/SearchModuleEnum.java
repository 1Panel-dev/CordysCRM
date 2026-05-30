package cn.cordys.crm.search.constants;

import java.util.List;

public class SearchModuleEnum {

    public static final String SEARCH_ADVANCED_CLUE = "searchAdvancedClue"; // 线索
    public static final String SEARCH_ADVANCED_CUSTOMER = "searchAdvancedCustomer"; // 客户
    public static final String SEARCH_ADVANCED_CONTACT = "searchAdvancedContact"; // 联系人
    public static final String SEARCH_ADVANCED_PUBLIC = "searchAdvancedPublic"; // 公海
    public static final String SEARCH_ADVANCED_CLUE_POOL = "searchAdvancedCluePool"; // 线索池
    public static final String SEARCH_ADVANCED_OPPORTUNITY = "searchAdvancedOpportunity"; // 商机


    public static final List<String> VALUES = List.of(
            SEARCH_ADVANCED_CLUE,
            SEARCH_ADVANCED_CLUE_POOL,
            SEARCH_ADVANCED_CUSTOMER,
            SEARCH_ADVANCED_PUBLIC,
            SEARCH_ADVANCED_OPPORTUNITY,
            SEARCH_ADVANCED_CONTACT
    );
}
