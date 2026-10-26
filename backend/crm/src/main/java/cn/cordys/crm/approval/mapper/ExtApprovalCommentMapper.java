package cn.cordys.crm.approval.mapper;

import cn.cordys.crm.approval.domain.ApprovalComment;
import cn.cordys.crm.approval.domain.ApprovalInstance;
import cn.cordys.crm.approval.dto.response.ApprovalCommentMentionUserResponse;
import cn.cordys.crm.approval.dto.response.ApprovalCommentResponse;
import cn.cordys.crm.approval.dto.response.ApprovalTodoItemResponse;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtApprovalCommentMapper {

    ApprovalInstance selectLatestInstance(@Param("resourceId") String resourceId,
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

    List<ApprovalCommentResponse> selectPage(@Param("resourceId") String resourceId,
                                           @Param("organizationId") String organizationId,
                                           @Param("asc") boolean asc);

    List<ApprovalCommentResponse> selectChildren(@Param("resourceId") String resourceId,
                                                 @Param("parentIds") List<String> parentIds,
                                                 @Param("organizationId") String organizationId,
                                                 @Param("asc") boolean asc);

    ApprovalCommentResponse selectDetail(@Param("id") String id,
                                         @Param("organizationId") String organizationId);

    List<ApprovalCommentMentionUserResponse> selectMentionUsers(@Param("commentIds") List<String> commentIds,
                                                               @Param("organizationId") String organizationId);

    boolean existsMention(@Param("resourceId") String resourceId,
                          @Param("userId") String userId,
                          @Param("organizationId") String organizationId);

    int deleteMentions(@Param("commentIds") List<String> commentIds,
                       @Param("organizationId") String organizationId);

    int deleteComments(@Param("commentIds") List<String> commentIds,
                       @Param("resourceId") String resourceId,
                       @Param("organizationId") String organizationId);

    List<String> selectEnabledUserIds(@Param("userIds") List<String> userIds,
                                      @Param("organizationId") String organizationId);

    boolean existsReplyUser(@Param("resourceId") String resourceId,
                            @Param("parentId") String parentId,
                            @Param("userId") String userId,
                            @Param("organizationId") String organizationId);
}
