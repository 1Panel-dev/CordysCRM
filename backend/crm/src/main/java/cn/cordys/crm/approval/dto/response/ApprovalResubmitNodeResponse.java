package cn.cordys.crm.approval.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApprovalResubmitNodeResponse {

    @Schema(description = "节点编号，例如P" + "N052")
    private String nodeNumber;

    @Schema(description = "普通审批节点名称，加签时返回宿主节点名称")
    private String nodeName;
}
