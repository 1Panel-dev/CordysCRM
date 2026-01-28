package cn.cordys.crm.search.controller;

import cn.cordys.aspectj.constants.GlobalSearchModule;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.dto.BasePageRequest;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.Pager;
import cn.cordys.common.pager.PagerWithOption;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.clue.dto.request.CluePageRequest;
import cn.cordys.crm.customer.dto.request.CustomerContactPageRequest;
import cn.cordys.crm.customer.dto.request.CustomerPageRequest;
import cn.cordys.crm.opportunity.dto.request.OpportunityPageRequest;
import cn.cordys.crm.search.response.advanced.*;
import cn.cordys.crm.search.service.BaseSearchService;
import cn.cordys.crm.search.service.advanced.AdvancedCustomerSearchService;
import cn.cordys.crm.search.service.advanced.AdvancedSearchServiceFactory;
import cn.cordys.crm.system.dto.request.RepeatCustomerDetailPageRequest;
import cn.cordys.security.SessionUtils;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import java.util.List;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/advanced/search")
@Tag(name = "全局高级搜索")
public class AdvancedSearchController {

  @Resource private AdvancedCustomerSearchService globalSearchCustomerService;

  @PostMapping("/account")
  @Operation(summary = "全局搜索客户相关数据")
  public PagerWithOption<List<AdvancedCustomerResponse>> advancedSearchCustomer(
      @Validated @RequestBody CustomerPageRequest request) {
    BaseSearchService<CustomerPageRequest, AdvancedCustomerResponse> searchService =
        AdvancedSearchServiceFactory.getSearchService(GlobalSearchModule.CUSTOMER);
    return searchService.startSearch(
        request, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
  }

  @PostMapping("/contact")
  @Operation(summary = "全局搜索联系人相关数据")
  public PagerWithOption<List<AdvancedCustomerContactResponse>> advancedSearchCustomerContact(
      @Validated @RequestBody CustomerContactPageRequest request) {
    BaseSearchService<CustomerContactPageRequest, AdvancedCustomerContactResponse> searchService =
        AdvancedSearchServiceFactory.getSearchService(GlobalSearchModule.CUSTOMER_CONTACT);
    return searchService.startSearch(
        request, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
  }

  @PostMapping("/lead")
  @Operation(summary = "全局搜索线索相关数据")
  public PagerWithOption<List<AdvancedClueResponse>> advancedSearchClue(
      @Validated @RequestBody CluePageRequest request) {
    BaseSearchService<CluePageRequest, AdvancedClueResponse> searchService =
        AdvancedSearchServiceFactory.getSearchService(GlobalSearchModule.CLUE);
    return searchService.startSearch(
        request, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
  }

  @PostMapping("/lead/detail")
  @Operation(summary = "全局搜索线索详情")
  @RequiresPermissions(
      value = {
        PermissionConstants.CLUE_MANAGEMENT_READ,
        PermissionConstants.CLUE_MANAGEMENT_POOL_READ
      },
      logical = Logical.OR)
  public Pager<List<AdvancedClueResponse>> getRepeatClueDetail(
      @Validated @RequestBody RepeatCustomerDetailPageRequest request) {
    Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
    return PageUtils.setPageInfo(
        page,
        globalSearchCustomerService.getRepeatClueDetail(
            request, OrganizationContext.getOrganizationId()));
  }

  @PostMapping("/opportunity/detail")
  @Operation(summary = "全局搜索商机详情")
  @RequiresPermissions(value = {PermissionConstants.OPPORTUNITY_MANAGEMENT_READ})
  public Pager<List<OpportunityRepeatResponse>> getRepeatOpportunityDetail(
      @Validated @RequestBody RepeatCustomerDetailPageRequest request) {
    Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
    return PageUtils.setPageInfo(
        page, globalSearchCustomerService.getRepeatOpportunityDetail(request));
  }

  @PostMapping("/opportunity")
  @Operation(summary = "全局搜索-商机")
  public PagerWithOption<List<AdvancedOpportunityResponse>> advancedSearchOpportunity(
      @Validated @RequestBody OpportunityPageRequest request) {
    BaseSearchService<OpportunityPageRequest, AdvancedOpportunityResponse> searchService =
        AdvancedSearchServiceFactory.getSearchService(GlobalSearchModule.OPPORTUNITY);
    return searchService.startSearch(
        request, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
  }

  @PostMapping("/account-pool")
  @Operation(summary = "全局搜索-公海")
  public PagerWithOption<List<AdvancedCustomerPoolResponse>> advancedSearchCustomerPool(
      @Validated @RequestBody BasePageRequest request) {
    BaseSearchService<BasePageRequest, AdvancedCustomerPoolResponse> searchService =
        AdvancedSearchServiceFactory.getSearchService(GlobalSearchModule.CUSTOMER_POOL);
    return searchService.startSearch(
        request, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
  }

  @PostMapping("/lead-pool")
  @Operation(summary = "全局搜索-线索池")
  public PagerWithOption<List<AdvancedCluePoolResponse>> advancedSearchCluePool(
      @Validated @RequestBody BasePageRequest request) {
    BaseSearchService<BasePageRequest, AdvancedCluePoolResponse> searchService =
        AdvancedSearchServiceFactory.getSearchService(GlobalSearchModule.CLUE_POOL);
    return searchService.startSearch(
        request, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
  }
}
