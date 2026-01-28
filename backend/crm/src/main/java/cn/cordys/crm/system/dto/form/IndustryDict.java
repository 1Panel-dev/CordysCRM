package cn.cordys.crm.system.dto.form;

import java.util.List;
import lombok.Data;

/**
 * @author song-cc-rock
 */
@Data
public class IndustryDict {

  private String label;
  private String value;
  private List<IndustryDict> children;
}
