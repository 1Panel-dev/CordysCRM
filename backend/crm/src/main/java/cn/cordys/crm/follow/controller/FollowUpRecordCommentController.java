package cn.cordys.crm.follow.controller;

import cn.cordys.common.pager.Pager;
import cn.cordys.context.OrganizationContext;
import cn.cordys.crm.follow.domain.FollowUpRecordComment;
import cn.cordys.crm.follow.dto.request.CommentAddRequest;
import cn.cordys.crm.follow.dto.request.CommentPageRequest;
import cn.cordys.crm.follow.dto.request.CommentUpdateRequest;
import cn.cordys.crm.follow.dto.response.CommentResponse;
import cn.cordys.crm.follow.service.FollowUpRecordCommentService;
import cn.cordys.crm.follow.service.FollowUpRecordService;
import cn.cordys.security.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "跟进记录评论")
@RestController
@RequestMapping("/follow/record/comment")
public class FollowUpRecordCommentController {

    @Resource
    private FollowUpRecordCommentService commentService;
    @Resource
    private FollowUpRecordService followUpRecordService;

    @PostMapping("/page")
    @Operation(summary = "分页查询跟进记录顶层评论")
    public Pager<List<CommentResponse>> page(@Validated @RequestBody CommentPageRequest request) {
        followUpRecordService.checkRecordPermission(request.getResourceId(), OrganizationContext.getOrganizationId(), SessionUtils.getUserId(), true);
        return commentService.page(request, OrganizationContext.getOrganizationId());
    }

    @PostMapping("/add")
    @Operation(summary = "添加跟进记录评论")
    public FollowUpRecordComment add(@Validated @RequestBody CommentAddRequest request) {
        followUpRecordService.checkRecordPermission(request.getResourceId(), OrganizationContext.getOrganizationId(), SessionUtils.getUserId(), false);
        return commentService.add(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @PostMapping("/update")
    @Operation(summary = "编辑跟进记录评论")
    public FollowUpRecordComment update(@Validated @RequestBody CommentUpdateRequest request) {
        return commentService.update(request, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }

    @GetMapping("/delete/{id}")
    @Operation(summary = "删除跟进记录评论")
    public void delete(@PathVariable String id) {
        commentService.delete(id, SessionUtils.getUserId(), OrganizationContext.getOrganizationId());
    }
}
