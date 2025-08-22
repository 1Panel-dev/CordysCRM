package io.cordys.crm.search.controller;

import io.cordys.aspectj.constants.GlobalSearchModule;
import io.cordys.common.dto.BasePageRequest;
import io.cordys.common.pager.Pager;
import io.cordys.common.pager.PagerWithOption;
import io.cordys.context.OrganizationContext;
import io.cordys.crm.search.response.global.GlobalOpportunityResponse;
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
    public Pager<List<GlobalOpportunityResponse>> advancedSearchOpportunity(@Validated @RequestBody BasePageRequest request) {
        BaseSearchService<BasePageRequest, GlobalOpportunityResponse> searchService = GlobalSearchServiceFactory.getSearchService(GlobalSearchModule.OPPORTUNITY);
        return searchService.startSearchNoOption(request, OrganizationContext.getOrganizationId(), SessionUtils.getUserId());
    }
}
