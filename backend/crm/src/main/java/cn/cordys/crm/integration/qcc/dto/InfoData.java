package cn.cordys.crm.integration.qcc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class InfoData {

    /**
     * 企业名称
     */
    @JsonProperty("Name")
    private String name;

    /**
     * 纳税人识别号
     */
    @JsonProperty("TaxNo")
    private String taxNo;


    /**
     * 开票信息
     */
    @JsonProperty("BankInfo")
    private BankInfo bankInfo;


    /**
     * 注册地址
     */
    @JsonProperty("Address")
    private String address;

    /**
     * 联系信息
     */
    @JsonProperty("ContactInfo")
    private ContactInfo contactInfo;


    /**
     * 注册资本
     */
    @JsonProperty("RegistCapi")
    private String registerCapi;


    /**
     * 公司规模
     */
    @JsonProperty("PersonScope")
    private String personScope;


    /**
     * 工商注册号
     */
    @JsonProperty("No")
    private String no;
}

