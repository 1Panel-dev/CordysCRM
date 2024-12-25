package io.cordys.aspectj.builder.parse;

import lombok.AllArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;

import java.util.Map;

@Setter
@AllArgsConstructor
public class OperationLogFunctionParser {
    public String getFunctionReturnValue(Map<String, String> beforeFunctionNameAndReturnMap, Object value, String expression, String functionName) {
        if (StringUtils.isEmpty(functionName)) {
            return value == null ? Strings.EMPTY : value.toString();
        }
        String functionCallInstanceKey = getFunctionCallInstanceKey(functionName, expression);
        if (beforeFunctionNameAndReturnMap != null && beforeFunctionNameAndReturnMap.containsKey(functionCallInstanceKey)) {
            return beforeFunctionNameAndReturnMap.get(functionCallInstanceKey);
        }
        return functionCallInstanceKey;
    }

    /**
     * @param functionName    函数名称
     * @param paramExpression 解析前的表达式
     * @return 函数缓存的key
     * 方法执行之前换成函数的结果，此时函数调用的唯一标志：函数名+参数表达式
     */
    public String getFunctionCallInstanceKey(String functionName, String paramExpression) {
        return functionName + paramExpression;
    }
}
