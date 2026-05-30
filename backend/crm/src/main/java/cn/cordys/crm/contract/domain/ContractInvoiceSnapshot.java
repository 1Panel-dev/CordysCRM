package cn.cordys.crm.contract.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;


@Data
@Table(name = "contract_invoice_snapshot")
public class ContractInvoiceSnapshot implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "ID", requiredMode = Schema.RequiredMode.REQUIRED)
	private String id;
	
	@Schema(description = "发票id")
	private String invoiceId;

	@Schema(description = "表单属性快照")
	private String invoiceProp;

	@Schema(description = "表单值快照")
	private String invoiceValue;
}
