package cn.cordys.aspectj.aop;

import lombok.Setter;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractBeanFactoryPointcutAdvisor;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.regex.Pattern;

/**
 * BeanFactoryLogRecordAdvisor with LogRecordPointcut
 * <p>
 * 结合了日志记录切点与通知的定义，提供一个完整的日志记录拦截功能。
 * 通过配置 `LogRecordOperationSource` 和自定义的 `LogRecordPointcut` 来判断
 * 哪些方法需要进行日志记录操作。
 * </p>
 */
@Setter
public class OperationLogAopAdvisor extends AbstractBeanFactoryPointcutAdvisor implements Serializable {

    /**
     * 日志记录操作源，用于获取方法的日志记录操作配置。
     */
    private OperationLogSource operationLogSource;

    /**
     * 获取日志记录的切点，用于定义拦截哪些方法。
     *
     * @return 返回定义的切点对象
     */
    @Override
    public Pointcut getPointcut() {
        return new LogRecordPointcut(operationLogSource);
    }

    /**
     * 自定义日志记录切点，用于判断哪些方法需要进行日志记录操作。
     */
    private static class LogRecordPointcut extends StaticMethodMatcherPointcut implements Serializable {

        private final OperationLogSource operationLogSource;

        public LogRecordPointcut(OperationLogSource operationLogSource) {
            this.operationLogSource = operationLogSource;
        }

        @Override
        public boolean matches(Method method, Class<?> targetClass) {
            // 定义支持通配符的包名模式
            // todo: 后续需要修改
            String packagePattern = "cn.cordys\\..*\\.service";

            // 获取目标类的包名
            String targetPackage = targetClass.getPackage().getName();

            // 使用正则表达式匹配包名
            if (!Pattern.matches(packagePattern, targetPackage)) {
                return false;
            }

            // 检查方法是否有日志操作配置
            return !CollectionUtils.isEmpty(operationLogSource.computeLogRecordOperations(method, targetClass));
        }
    }
}
