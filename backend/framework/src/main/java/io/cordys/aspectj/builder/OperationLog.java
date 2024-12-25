package io.cordys.aspectj.builder;

import io.cordys.aspectj.constants.CodeVariableType;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.validator.constraints.Length;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class OperationLog {

    /**
     * id
     */
    private Serializable id;
    /**
     * 保存的操作日志的类型，比如：订单类型、商品类型
     **/
    @NotBlank(message = "type required")
    @Length(max = 200, message = "type max length is 200")
    private String type;

    /**
     * 日志的子类型，比如订单的C端日志，和订单的B端日志，type都是订单类型，但是子类型不一样
     */
    private String subType;

    /**
     * 日志绑定的业务标识
     */
    @NotBlank(message = "resourceId required")
    @Length(max = 100, message = "resourceId max length is 100")
    private String resourceId;

    /**
     * 操作人
     */
    @NotBlank(message = "operator required")
    @Length(max = 50, message = "operator max length 50")
    private String operator;

    /**
     * 日志内容
     */
    @NotBlank(message = "opAction required")
    @Length(max = 500, message = "operator max length 500")
    private String action;

    /**
     * 记录是否是操作失败的日志
     */
    private boolean fail;

    /**
     * 日志的创建时间
     */
    private Date createTime;

    /**
     * 日志的额外信息
     **/
    private String extra;

    /**
     * 打印日志的代码信息
     * CodeVariableType 日志记录的ClassName、MethodName
     */
    private Map<CodeVariableType, Object> codeVariable;
}