package cn.cordys.crm.clue.domain;

import cn.cordys.common.domain.BaseResourceField;
import jakarta.persistence.Table;
import lombok.Data;


/**
 * 线索自定义属性大文本
 * 
 * @author jianxing
 * @date 2025-02-27 14:43:46
 */
@Data
@Table(name = "clue_field_blob")
public class ClueFieldBlob extends BaseResourceField {
}
