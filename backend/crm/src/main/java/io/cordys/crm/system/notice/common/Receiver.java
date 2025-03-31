package io.cordys.crm.system.notice.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@Data
@AllArgsConstructor
@EqualsAndHashCode
public class Receiver implements Serializable {
    private String userId;
    private String type;
}
