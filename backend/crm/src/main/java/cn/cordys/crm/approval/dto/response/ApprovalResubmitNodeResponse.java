package cn.cordys.crm.approval.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApprovalResubmitNodeResponse {

    @Schema(description = "节点编号，首项为空字符串，表示整单重审；普通节点例如P" + "N052")
    private String nodeNumber;

    @Schema(description = "节点名称，首项为本轮审批实例提交人的姓名，加签时返回宿主节点名称")
    private String nodeName;
}
