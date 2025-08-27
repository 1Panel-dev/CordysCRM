package cn.cordys.aspectj.dto;

import cn.cordys.common.util.JSON;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogContextInfo implements java.io.Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * 原始值
     */
    private Object originalValue;

    /**
     * 修改后的值
     */
    private Object modifiedValue;

    /**
     * 资源的id
     * 优先级高于注解
     */
    private String resourceId;

    /**
     * 资源的名称
     * 优先级高于注解
     */
    private String resourceName;

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }

}
