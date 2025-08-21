package io.cordys.crm.search.controller;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.cordys.aspectj.constants.GlobalSearchModule;
import io.cordys.common.constants.PermissionConstants;
import io.cordys.common.dto.BasePageRequest;
import io.cordys.common.pager.PageUtils;
import io.cordys.common.pager.Pager;
import io.cordys.common.pager.PagerWithOption;
import io.cordys.context.OrganizationContext;
import io.cordys.crm.clue.dto.request.CluePageRequest;
import io.cordys.crm.customer.dto.request.CustomerContactPageRequest;
import io.cordys.crm.customer.dto.request.CustomerPageRequest;
import io.cordys.crm.opportunity.dto.request.OpportunityPageRequest;
import io.cordys.crm.search.response.advanced.*;
import io.cordys.crm.search.service.BaseSearchService;
import io.cordys.crm.search.service.advanced.AdvancedCustomerSearchService;
import io.cordys.crm.search.service.advanced.AdvancedSearchServiceFactory;
import io.cordys.crm.system.dto.request.RepeatCustomerDetailPageRequest;
import io.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/advanced/search")
@Tag(name = "全局高级搜索")
public class AdvancedSearchController {

    @Resource
    private AdvancedCustomerSearchService globalSearchCustomerService;

    @PostMapping("/customer")
    @Operation(summary = "全局搜索客户相关数据")
    public PagerWithOption<List<AdvancedCustomerResponse>> advancedSearchCustomer(@Validated @RequestBody CustomerPageRequest request) {
        BaseSearchService<CustomerPageRequest, AdvancedCustomerResponse> searchService = AdvancedSearchServiceFactory.getSearchService(GlobalSearchModule.CUSTOMER);
        return searchService.startSearch(request, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }


    @PostMapping("/contact")
    @Operation(summary = "全局搜索联系人相关数据")
    public PagerWithOption<List<AdvancedCustomerContactResponse>> advancedSearchCustomerContact(@Validated @RequestBody CustomerContactPageRequest request) {
        BaseSearchService<CustomerContactPageRequest, AdvancedCustomerContactResponse> searchService = AdvancedSearchServiceFactory.getSearchService(GlobalSearchModule.CUSTOMER_CONTACT);
        return searchService.startSearch(request, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }


    @PostMapping("/clue")
    @Operation(summary = "全局搜索线索相关数据")
    public PagerWithOption<List<AdvancedClueResponse>> advancedSearchClue(@Validated @RequestBody CluePageRequest request) {
        BaseSearchService<CluePageRequest, AdvancedClueResponse> searchService = AdvancedSearchServiceFactory.getSearchService(GlobalSearchModule.CLUE);
        return searchService.startSearch(request, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }


    @PostMapping("/clue/detail")
    @Operation(summary = "全局搜索线索详情")
    @RequiresPermissions(value = {PermissionConstants.CLUE_MANAGEMENT_READ, PermissionConstants.CLUE_MANAGEMENT_POOL_READ}, logical = Logical.OR)
    public Pager<List<AdvancedClueResponse>> getRepeatClueDetail(@Validated @RequestBody RepeatCustomerDetailPageRequest request) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        return PageUtils.setPageInfo(page, globalSearchCustomerService.getRepeatClueDetail(request, OrganizationContext.getOrganizationId()));
    }

    @PostMapping("/opportunity/detail")
    @Operation(summary = "全局搜索商机详情")
    @RequiresPermissions(value = {PermissionConstants.OPPORTUNITY_MANAGEMENT_READ})
    public Pager<List<OpportunityRepeatResponse>> getRepeatOpportunityDetail(@Validated @RequestBody RepeatCustomerDetailPageRequest request) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        return PageUtils.setPageInfo(page, globalSearchCustomerService.getRepeatOpportunityDetail(request));
    }


    @PostMapping("/opportunity")
    @Operation(summary = "全局搜索-商机")
    public PagerWithOption<List<AdvancedOpportunityResponse>> advancedSearchOpportunity(@Validated @RequestBody OpportunityPageRequest request) {
        BaseSearchService<OpportunityPageRequest, AdvancedOpportunityResponse> searchService = AdvancedSearchServiceFactory.getSearchService(GlobalSearchModule.OPPORTUNITY);
        return searchService.startSearch(request, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }


    @PostMapping("/customer-pool")
    @Operation(summary = "全局搜索-公海")
    public PagerWithOption<List<AdvancedCustomerPoolResponse>> advancedSearchCustomerPool(@Validated @RequestBody BasePageRequest request) {
        BaseSearchService<BasePageRequest, AdvancedCustomerPoolResponse> searchService = AdvancedSearchServiceFactory.getSearchService(GlobalSearchModule.CUSTOMER_POOL);
        return searchService.startSearch(request, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }

    @PostMapping("/clue-pool")
    @Operation(summary = "全局搜索-线索池")
    public PagerWithOption<List<AdvancedCluePoolResponse>> advancedSearchCluePool(@Validated @RequestBody BasePageRequest request) {
        BaseSearchService<BasePageRequest, AdvancedCluePoolResponse> searchService = AdvancedSearchServiceFactory.getSearchService(GlobalSearchModule.CLUE_POOL);
        return searchService.startSearch(request, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }
}
