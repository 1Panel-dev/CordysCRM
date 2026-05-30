package cn.cordys.crm.integration.qcc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class BankInfo {


    /**
     * 开户银行
     */
    @JsonProperty("Bank")
    private String bank;

    /**
     * 开户行账号
     */
    @JsonProperty("BankAccount")
    private String bankAccount;


}
