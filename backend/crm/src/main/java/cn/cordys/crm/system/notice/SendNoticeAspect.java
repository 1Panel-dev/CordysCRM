package cn.cordys.crm.system.notice;

import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.common.util.JSON;
import cn.cordys.context.OrganizationContext;
import cn.cordys.security.SessionUtils;
import jakarta.annotation.Resource;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanMap;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.StandardReflectionParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class SendNoticeAspect {
  private static final String ID = "id";
  private static final String CREATE_USER = "createUser";
  private static final String CREATE_TIME = "createTime";
  private static final String UPDATE_TIME = "updateTime";
  private static final String UPDATE_USER = "updateUser";
  private final ExpressionParser parser = new SpelExpressionParser();
  private final StandardReflectionParameterNameDiscoverer discoverer =
      new StandardReflectionParameterNameDiscoverer();
  private final ThreadLocal<String> source = new ThreadLocal<>();
  @Resource private CommonNoticeSendService commonNoticeSendService;

  @Pointcut("@annotation(cn.cordys.crm.system.notice.SendNotice)")
  public void pointcut() {}

  @Before("pointcut()")
  public void before(JoinPoint joinPoint) {
    try {
      // 从切面织入点处通过反射机制获取织入点处的方法
      MethodSignature signature = (MethodSignature) joinPoint.getSignature();
      // 获取切入点所在的方法
      Method method = signature.getMethod();
      // 获取参数对象数组
      Object[] args = joinPoint.getArgs();
      SendNotice sendNotice = method.getAnnotation(SendNotice.class);

      if (StringUtils.isNotEmpty(sendNotice.target())) {
        // 操作内容
        // 获取方法参数名
        String[] params = discoverer.getParameterNames(method);
        // 将参数纳入Spring管理
        EvaluationContext context = new StandardEvaluationContext();
        for (int len = 0; len < Objects.requireNonNull(params).length; len++) {
          context.setVariable(params[len], args[len]);
        }
        context.setVariable("targetClass", CommonBeanFactory.getBean(sendNotice.targetClass()));

        String target = sendNotice.target();
        Expression titleExp = parser.parseExpression(target);
        Object v = titleExp.getValue(context, Object.class);
        source.set(JSON.toJSONString(v));
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  @AfterReturning(value = "pointcut()", returning = "retValue")
  public void sendNotice(JoinPoint joinPoint, Object retValue) {
    try {
      MethodSignature signature = (MethodSignature) joinPoint.getSignature();
      Method method = signature.getMethod();
      Object[] args = joinPoint.getArgs();
      String[] params = discoverer.getParameterNames(method);

      SendNotice sendNotice = method.getAnnotation(SendNotice.class);
      EvaluationContext context = new StandardEvaluationContext();

      if (params != null) {
        for (int i = 0; i < params.length; i++) {
          context.setVariable(params[i], args[i]);
        }
      }

      // 获取目标对象最新数据
      if (StringUtils.isNotEmpty(sendNotice.target())) {
        Object targetBean = CommonBeanFactory.getBean(sendNotice.targetClass());
        context.setVariable("targetClass", targetBean);

        Object targetValue =
            parser.parseExpression(sendNotice.target()).getValue(context, Object.class);

        String jsonValue = JSON.toJSONString(targetValue);
        if (targetValue != null
            && !Strings.CS.equals("{}", jsonValue)
            && !Strings.CS.equals("[]", jsonValue)) {
          source.set(jsonValue);
        }
      }

      // 组装资源列表
      var resources = new ArrayList<Map>();
      String src = source.get();
      if (StringUtils.isNotBlank(src)) {
        if (Strings.CS.startsWith(src, "[")) {
          resources.addAll(JSON.parseArray(src, Map.class));
        } else {
          resources.add(JSON.parseObject(src, Map.class));
        }
      } else {
        resources.add(new BeanMap(retValue));
      }

      // 解析模块名
      String module = sendNotice.module();
      if (StringUtils.isNotEmpty(module)) {
        try {
          module = parser.parseExpression(module).getValue(resources, String.class);
        } catch (Exception e) {
          log.info("使用原值 module:{}", module);
        }
      }

      // 解析事件名
      String event = sendNotice.event();
      if (StringUtils.isNotEmpty(event)) {
        try {
          event = parser.parseExpression(event).getValue(context, String.class);
        } catch (Exception e) {
          log.info("使用原值 event:{}", event);
        }
      }

      log.info("event:{}", event);

      // 补全基础字段
      var object = JSON.parseMap(JSON.toJSONString(retValue));
      if (MapUtils.isNotEmpty(object)) {
        List.of(ID, CREATE_USER, CREATE_TIME, UPDATE_TIME, UPDATE_USER)
            .forEach(
                key ->
                    resources.forEach(
                        resource -> {
                          if (object.containsKey(key) && resource.get(key) == null) {
                            resource.put(key, object.get(key));
                          }
                        }));
      }

      commonNoticeSendService.sendNotice(
          module,
          event,
          resources,
          SessionUtils.getUserId(),
          OrganizationContext.getOrganizationId());

    } catch (Exception e) {
      log.error(e.getMessage(), e);
    } finally {
      source.remove();
    }
  }
}
