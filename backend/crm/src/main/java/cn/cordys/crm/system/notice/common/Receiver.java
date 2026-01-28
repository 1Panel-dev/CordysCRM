package cn.cordys.crm.system.notice.common;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@AllArgsConstructor
@EqualsAndHashCode
public class Receiver implements Serializable {
  private String userId;
  private String type;
}
