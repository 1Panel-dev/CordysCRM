package cn.cordys.crm.contract.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

@Data
@Table(name = "contract_snapshot")
public class ContractSnapshot implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  @Schema(description = "ID", requiredMode = Schema.RequiredMode.REQUIRED)
  private String id;

  @Schema(description = "合同id")
  private String contractId;

  @Schema(description = "表单属性快照")
  private String contractProp;

  @Schema(description = "表单值快照")
  private String contractValue;
}
