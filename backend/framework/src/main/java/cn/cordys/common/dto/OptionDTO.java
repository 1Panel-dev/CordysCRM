package cn.cordys.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OptionDTO implements Serializable {
    @Schema(description = "选项ID")
    private Object id;
    @Schema(description = "选项名称")
    private String name;

    /**
     * 获取字符串类型的ID（用于内部逻辑）
     */
    public String getIdAsString() {
        return id == null ? null : String.valueOf(id);
    }
}
