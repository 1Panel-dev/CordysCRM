package cn.cordys.crm.follow.controller;

import cn.cordys.common.pager.PagerWithCommentCount;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.follow.domain.FollowUpPlanComment;
import cn.cordys.crm.follow.dto.request.CommentAddRequest;
import cn.cordys.crm.follow.dto.request.CommentPageRequest;
import cn.cordys.crm.follow.dto.request.CommentUpdateRequest;
import cn.cordys.crm.follow.dto.response.CommentResponse;
import cn.cordys.crm.follow.service.FollowUpPlanCommentService;
import cn.cordys.crm.follow.service.FollowUpPlanService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "跟进计划评论")
@RestController
@RequestMapping("/follow/plan/comment")
public class FollowUpPlanCommentController {

    @Resource
    private FollowUpPlanCommentService commentService;
    @Resource
    private FollowUpPlanService followUpPlanService;

    @PostMapping("/page")
    @Operation(summary = "分页查询跟进计划顶层评论")
    public PagerWithCommentCount<List<CommentResponse>> page(@Validated @RequestBody CommentPageRequest request) {
        followUpPlanService.checkPlanPermission(request.getResourceId(), OrganizationContext.getOrganizationId(), SessionUtils.getUserId(), true);
        return commentService.page(request, OrganizationContext.getOrganizationId());
    }

    @PostMapping("/add")
    @Operation(summary = "添加跟进计划评论")
    public FollowUpPlanComment add(@Validated @RequestBody CommentAddRequest request) {
        followUpPlanService.checkPlanPermission(request.getResourceId(), OrganizationContext.getOrganizationId(), SessionUtils.getUserId(), true);
        return commentService.add(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/update")
    @Operation(summary = "编辑跟进计划评论")
    public FollowUpPlanComment update(@Validated @RequestBody CommentUpdateRequest request) {
        // 方法里校验了权限
        return commentService.update(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @GetMapping("/delete/{id}")
    @Operation(summary = "删除跟进计划评论")
    public FollowUpPlanComment delete(@PathVariable String id) {
        // 方法里校验了权限
        return commentService.delete(id, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }
}
