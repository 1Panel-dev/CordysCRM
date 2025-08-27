package cn.cordys.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OptionCountDTO implements Serializable {
    @Schema(description =  "选项数量")
    private String key;
    @Schema(description =  "选项值")
    private Integer count;
}
