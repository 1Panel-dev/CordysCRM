package cn.cordys.crm.integration.dataease.dto;

import java.util.List;
import lombok.Data;

/**
 * @Author: jianxing @CreateTime: 2025-08-20 13:56
 */
@Data
public class PageDTO<T> {
  private Integer current;
  private Integer pages;
  private Integer size;
  private Integer total;
  private List<T> records;
}
