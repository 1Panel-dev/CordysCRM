package io.cordys.aspectj.dto;

import io.cordys.common.util.JSON;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogExtraDTO implements java.io.Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * 原始值（字节数组）
     */
    private Object originalValue;

    /**
     * 修改后的值（字节数组）
     */
    private Object modifiedValue;

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }

}
