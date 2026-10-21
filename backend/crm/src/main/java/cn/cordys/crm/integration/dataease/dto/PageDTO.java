package cn.cordys.crm.integration.dataease.dto;

import lombok.Data;

import java.util.List;

/**
 * @author jianxing
 */
@Data
public class PageDTO<T> {
    private Integer current;
    private Integer pages;
    private Integer size;
    private Integer total;
    private List<T> records;
}
