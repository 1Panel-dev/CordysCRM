package cn.cordys.crm.clue.domain;

import cn.cordys.common.domain.BaseResourceField;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 线索自定义属性
 *
 * @author jianxing
 */
@Data
@Table(name = "clue_field")
public class ClueField extends BaseResourceField {
}
