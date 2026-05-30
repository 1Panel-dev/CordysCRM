package cn.cordys.crm.integration.qcc.constant;

public interface QccApiPaths {

    String FUZZY_SEARCH_API = "/FuzzySearch/GetList?key={0}&searchKey={1}";
    String ENTERPRISE_INFO_VERIFY_API = "/EnterpriseInfo/Verify?key={0}&searchKey={1}";

    String FUZZY_SEARCH_LIST_API = "/FuzzySearch/GetList?key={0}&searchKey={1}&pageIndex={2}";
    
}
