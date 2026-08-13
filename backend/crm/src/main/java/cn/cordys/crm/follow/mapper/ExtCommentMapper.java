package cn.cordys.crm.follow.mapper;

import cn.cordys.crm.follow.domain.CommentMention;
import cn.cordys.crm.follow.dto.response.CommentResponse;
import cn.cordys.crm.follow.dto.response.CommentMentionUserResponse;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtCommentMapper {

    List<CommentResponse> selectPage(@Param("targetType") String targetType,
                                     @Param("targetId") String targetId,
                                     @Param("parentId") String parentId,
                                     @Param("organizationId") String organizationId);

    List<CommentResponse> selectChildren(@Param("targetType") String targetType,
                                         @Param("parentIds") List<String> parentIds,
                                         @Param("organizationId") String organizationId);

    long countByResource(@Param("targetType") String targetType,
                         @Param("resourceId") String resourceId,
                         @Param("organizationId") String organizationId);

    List<CommentMentionUserResponse> selectMentionUsers(@Param("commentMentionTable") String commentMentionTable,
                                                        @Param("commentIds") List<String> commentIds,
                                                        @Param("organizationId") String organizationId);

    int batchInsertMentionUsers(@Param("commentMentionTable") String commentMentionTable,
                                @Param("mentions") List<CommentMention> mentions);

    int deleteMentionUsers(@Param("commentMentionTable") String commentMentionTable,
                           @Param("commentIds") List<String> commentIds);
}
