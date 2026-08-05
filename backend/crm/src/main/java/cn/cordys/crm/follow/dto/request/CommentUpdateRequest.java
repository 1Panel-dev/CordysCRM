package cn.cordys.crm.follow.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CommentUpdateRequest {

    @NotBlank
    private String id;

    @NotBlank
    @Size(max = 3000)
    private String content;

    @Size(max = 100)
    @Schema(description = "被@用户ID")
    private List<String> mentionedUserIds;
}
