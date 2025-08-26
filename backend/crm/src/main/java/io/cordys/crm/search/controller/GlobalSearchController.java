package io.cordys.crm.search.controller;

import io.cordys.aspectj.constants.GlobalSearchModule;
import io.cordys.common.dto.BasePageRequest;
import io.cordys.common.pager.Pager;
import io.cordys.context.OrganizationContext;
import io.cordys.crm.search.response.global.*;
import io.cordys.crm.search.service.BaseSearchService;
import io.cordys.crm.search.service.global.GlobalSearchServiceFactory;
import io.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/global/search")
@Tag(name = "全局搜索")
public class GlobalSearchController {

    @PostMapping("/opportunity")
    @Operation(summary = "全局搜索-商机")
    public Pager<List<GlobalOpportunityResponse>> globalSearchOpportunity(@Validated @RequestBody BasePageRequest request) {
        BaseSearchService<BasePageRequest, GlobalOpportunityResponse> searchService = GlobalSearchServiceFactory.getSearchService(GlobalSearchModule.OPPORTUNITY);
        return searchService.startSearchNoOption(request, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }

    @PostMapping("/customer_pool")
    @Operation(summary = "全局搜索-公海")
    public Pager<List<GlobalCustomerPoolResponse>> globalSearchCustomerPool(@Validated @RequestBody BasePageRequest request) {
        BaseSearchService<BasePageRequest, GlobalCustomerPoolResponse> searchService = GlobalSearchServiceFactory.getSearchService(GlobalSearchModule.CUSTOMER_POOL);
        return searchService.startSearchNoOption(request, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }

    @PostMapping("/clue")
    @Operation(summary = "全局搜索-线索")
    public Pager<List<GlobalClueResponse>> globalSearchClue(@Validated @RequestBody BasePageRequest request) {
        BaseSearchService<BasePageRequest, GlobalClueResponse> searchService = GlobalSearchServiceFactory.getSearchService(GlobalSearchModule.CLUE);
        return searchService.startSearchNoOption(request, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }

    @PostMapping("/clue_pool")
    @Operation(summary = "全局搜索-线索池")
    public Pager<List<GlobalCluePoolResponse>> globalSearchCluePool(@Validated @RequestBody BasePageRequest request) {
        BaseSearchService<BasePageRequest, GlobalCluePoolResponse> searchService = GlobalSearchServiceFactory.getSearchService(GlobalSearchModule.CLUE_POOL);
        return searchService.startSearchNoOption(request, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }

    @PostMapping("/customer")
    @Operation(summary = "全局搜索-客户")
    public Pager<List<GlobalCustomerResponse>> globalSearchCustomer(@Validated @RequestBody BasePageRequest request) {
        BaseSearchService<BasePageRequest, GlobalCustomerResponse> searchService = GlobalSearchServiceFactory.getSearchService(GlobalSearchModule.CUSTOMER);
        return searchService.startSearchNoOption(request, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }

    @PostMapping("/contact")
    @Operation(summary = "全局搜索-联系人")
    public Pager<List<GlobalCustomerContactResponse>> globalSearchContact(@Validated @RequestBody BasePageRequest request) {
        BaseSearchService<BasePageRequest, GlobalCustomerContactResponse> searchService = GlobalSearchServiceFactory.getSearchService(GlobalSearchModule.CUSTOMER_CONTACT);
        return searchService.startSearchNoOption(request, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }
}
