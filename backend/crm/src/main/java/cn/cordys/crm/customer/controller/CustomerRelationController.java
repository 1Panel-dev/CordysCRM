package cn.cordys.crm.customer.controller;

import cn.cordys.common.constants.FormKeyConstants;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.permission.CsPermission;
import cn.cordys.crm.customer.domain.CustomerRelation;
import cn.cordys.crm.customer.dto.request.CustomerRelationSaveRequest;
import cn.cordys.crm.customer.dto.request.CustomerRelationUpdateRequest;
import cn.cordys.crm.customer.dto.response.CustomerRelationListResponse;
import cn.cordys.crm.customer.service.CustomerRelationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author jianxing
 */
@Tag(name = "客户关系")
@RestController
@RequestMapping("/account/relation")
public class CustomerRelationController {
    @Resource
    private CustomerRelationService customerRelationService;

    @GetMapping("/list/{customerId}")
    @CsPermission(value = PermissionConstants.CUSTOMER_MANAGEMENT_READ, resourceId = "{#customerId}", formType = FormKeyConstants.CUSTOMER)
    @Operation(summary = "客户关系列表")
    public List<CustomerRelationListResponse> list(@PathVariable String customerId) {
        return customerRelationService.list(customerId);
    }

    @PostMapping("/add/{customerId}")
    @CsPermission(value = PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE, resourceId = "{#customerId}", formType = FormKeyConstants.CUSTOMER)
    @Operation(summary = "添加客户关系")
    public CustomerRelation add(@PathVariable String customerId,
            @Validated(CustomerRelationSaveRequest.SingleOperation.class)
            @RequestBody CustomerRelationSaveRequest request) {
        return customerRelationService.add(request, customerId);
    }

    @PostMapping("/update/{customerId}")
    @CsPermission(value = PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE, resourceId = "{#customerId}", formType = FormKeyConstants.CUSTOMER)
    @Operation(summary = "更新客户关系")
    public CustomerRelation update(@PathVariable String customerId,
            @Validated(CustomerRelationSaveRequest.SingleOperation.class)
            @RequestBody CustomerRelationUpdateRequest request) {
        return customerRelationService.update(request, customerId);
    }

    @GetMapping("/delete/{id}")
    @CsPermission(PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE)
    @Operation(summary = "删除客户关系")
    public void delete(@PathVariable String id) {
        customerRelationService.delete(id);
    }

    @PostMapping("/save/{customerId}")
    @CsPermission(value = PermissionConstants.CUSTOMER_MANAGEMENT_UPDATE, resourceId = "{#customerId}", formType = FormKeyConstants.CUSTOMER)
    @Operation(summary = "整体客户关系")
    public void save(@PathVariable String customerId,
            @Validated @RequestBody @Valid @NotNull List<CustomerRelationSaveRequest> requests) {
        customerRelationService.save(customerId, requests);
    }
}
