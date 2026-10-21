package cn.cordys.crm.approval.mapper;

import cn.cordys.crm.approval.domain.ApprovalComment;
import cn.cordys.crm.approval.domain.ApprovalInstance;
import cn.cordys.crm.approval.dto.response.ApprovalCommentMentionUserResponse;
import cn.cordys.crm.approval.dto.response.ApprovalCommentResponse;
import cn.cordys.crm.approval.dto.response.ApprovalTodoItemResponse;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtApprovalCommentMapper {

    ApprovalInstance selectInstance(@Param("instanceId") String instanceId,
                                    @Param("organizationId") String organizationId);

    List<ApprovalTodoItemResponse> selectMentionTodos(@Param("userId") String userId,
                                                     @Param("organizationId") String organizationId,
                                                     @Param("resourceType") String resourceType,
                                                     @Param("keyword") String keyword);

    ApprovalInstance selectMentionInstance(@Param("mentionId") String mentionId,
                                            @Param("userId") String userId,
                                            @Param("organizationId") String organizationId);

    ApprovalComment selectCommentForUpdate(@Param("id") String id,
                                           @Param("organizationId") String organizationId);

    List<ApprovalCommentResponse> selectList(@Param("instanceId") String instanceId,
                                             @Param("organizationId") String organizationId);

    List<ApprovalCommentResponse> selectChildren(@Param("instanceId") String instanceId,
                                                 @Param("parentIds") List<String> parentIds,
                                                 @Param("organizationId") String organizationId);

    ApprovalCommentResponse selectDetail(@Param("id") String id,
                                         @Param("organizationId") String organizationId);

    List<ApprovalCommentMentionUserResponse> selectMentionUsers(@Param("commentIds") List<String> commentIds,
                                                               @Param("organizationId") String organizationId);

    boolean existsMention(@Param("instanceId") String instanceId,
                          @Param("userId") String userId,
                          @Param("organizationId") String organizationId);

    int deleteMentions(@Param("commentIds") List<String> commentIds,
                       @Param("organizationId") String organizationId);

    int deleteComments(@Param("commentIds") List<String> commentIds,
                       @Param("instanceId") String instanceId,
                       @Param("organizationId") String organizationId);

    List<String> selectEnabledUserIds(@Param("userIds") List<String> userIds,
                                      @Param("organizationId") String organizationId);

    boolean existsReplyUser(@Param("instanceId") String instanceId,
                            @Param("parentId") String parentId,
                            @Param("userId") String userId,
                            @Param("organizationId") String organizationId);
}
