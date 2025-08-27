package cn.cordys.crm.system.dto.convert;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class AnnouncementContentDTO {

    @Schema(description = "链接")
    private String url;

    @Schema(description = "重命名链接")
    private String renameUrl;

    @Schema(description = "公告内容")
    private String content;
}
