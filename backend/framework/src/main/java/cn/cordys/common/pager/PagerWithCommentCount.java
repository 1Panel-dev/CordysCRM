package cn.cordys.common.pager;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class PagerWithCommentCount<T> extends Pager<T> {

    @Schema(description = "资源下的评论数量")
    private Long commentCount;
}
