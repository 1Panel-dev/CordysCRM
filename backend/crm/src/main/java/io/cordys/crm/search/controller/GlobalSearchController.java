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
import io.cordys.crm.search.response.*;
import io.cordys.crm.search.service.GlobalSearchBaseService;
import io.cordys.crm.search.service.GlobalSearchCustomerService;
import io.cordys.crm.search.service.GlobalSearchServiceFactory;
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
@RequestMapping("/global/search")
@Tag(name = "全局搜索")
public class GlobalSearchController {

    @Resource
    private GlobalSearchCustomerService globalSearchCustomerService;

    @PostMapping("/customer")
    @Operation(summary = "全局搜索客户相关数据")
    public PagerWithOption<List<GlobalCustomerResponse>> getRepeatCustomer(@Validated @RequestBody CustomerPageRequest request) {
        GlobalSearchBaseService<CustomerPageRequest, GlobalCustomerResponse> searchService = GlobalSearchServiceFactory.getSearchService(GlobalSearchModule.CUSTOMER);
        return searchService.globalSearch(request, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }


    @PostMapping("/contact")
    @Operation(summary = "全局搜索联系人相关数据")
    public PagerWithOption<List<GlobalCustomerContactResponse>> getRepeatCustomerContact(@Validated @RequestBody CustomerContactPageRequest request) {
        GlobalSearchBaseService<CustomerContactPageRequest, GlobalCustomerContactResponse> searchService = GlobalSearchServiceFactory.getSearchService(GlobalSearchModule.CUSTOMER_CONTACT);
        return searchService.globalSearch(request, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }


    @PostMapping("/clue")
    @Operation(summary = "全局搜索线索相关数据")
    public PagerWithOption<List<GlobalClueResponse>> getRepeatClue(@Validated @RequestBody CluePageRequest request) {
        GlobalSearchBaseService<CluePageRequest, GlobalClueResponse> searchService = GlobalSearchServiceFactory.getSearchService(GlobalSearchModule.CLUE);
        return searchService.globalSearch(request, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }


    @PostMapping("/clue/detail")
    @Operation(summary = "全局搜索线索详情")
    @RequiresPermissions(value = {PermissionConstants.CLUE_MANAGEMENT_READ, PermissionConstants.CLUE_MANAGEMENT_POOL_READ}, logical = Logical.OR)
    public Pager<List<GlobalClueResponse>> getRepeatClueDetail(@Validated @RequestBody RepeatCustomerDetailPageRequest request) {
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
    @RequiresPermissions(value = {PermissionConstants.OPPORTUNITY_MANAGEMENT_READ})
    public PagerWithOption<List<GlobalOpportunityResponse>> globalSearchOpportunity(@Validated @RequestBody OpportunityPageRequest request) {
        GlobalSearchBaseService<OpportunityPageRequest, GlobalOpportunityResponse> searchService = GlobalSearchServiceFactory.getSearchService(GlobalSearchModule.OPPORTUNITY);
        return searchService.globalSearch(request, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }


    @PostMapping("/customer-pool")
    @Operation(summary = "全局搜索-公海")
    public PagerWithOption<List<GlobalCustomerPoolResponse>> globalSearchCustomerPool(@Validated @RequestBody BasePageRequest request) {
        GlobalSearchBaseService<BasePageRequest, GlobalCustomerPoolResponse> searchService = GlobalSearchServiceFactory.getSearchService(GlobalSearchModule.CUSTOMER_POOL);
        return searchService.globalSearch(request, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }

    @PostMapping("/clue-pool")
    @Operation(summary = "全局搜索-线索池")
    public PagerWithOption<List<GlobalCluePoolResponse>> globalSearchCluePool(@Validated @RequestBody BasePageRequest request) {
        GlobalSearchBaseService<BasePageRequest, GlobalCluePoolResponse> searchService = GlobalSearchServiceFactory.getSearchService(GlobalSearchModule.CLUE_POOL);
        return searchService.globalSearch(request, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }
}
