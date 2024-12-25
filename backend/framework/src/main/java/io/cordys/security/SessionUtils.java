package io.cordys.security;

import io.cordys.common.util.CommonBeanFactory;
import io.cordys.common.util.LogUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.data.redis.RedisIndexedSessionRepository;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;
import java.util.Objects;

import static io.cordys.security.SessionConstants.ATTR_USER;


/**
 * Session 工具类，提供操作用户 Session 的常用方法。
 * <p>
 * 包含获取当前用户信息、获取 Session ID、踢除用户等功能。
 * </p>
 */
public class SessionUtils {

    private static final ThreadLocal<String> organizationId = new ThreadLocal<>();

    /**
     * 获取当前用户的 ID。
     *
     * @return 当前用户的 ID，如果没有获取到用户信息，则返回 null
     */
    public static String getUserId() {
        SessionUser user = getUser();
        return user == null ? null : user.getId();
    }

    /**
     * 获取当前用户信息。
     *
     * @return 当前用户对象，如果未获取到用户信息，则返回 null
     */
    public static SessionUser getUser() {
        try {
            Subject subject = SecurityUtils.getSubject();
            Session session = subject.getSession();
            return (SessionUser) session.getAttribute(ATTR_USER);
        } catch (Exception e) {
            LogUtils.warn("后台获取在线用户失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 获取当前 Session 的 ID。
     *
     * @return 当前 Session 的 ID
     */
    public static String getSessionId() {
        return (String) SecurityUtils.getSubject().getSession().getId();
    }

    /**
     * 踢除指定用户名的用户（从 Redis 会话中删除）。
     *
     * @param username 用户名
     */
    public static void kickOutUser(String username) {
        // 获取 Redis session 存储库
        RedisIndexedSessionRepository sessionRepository = CommonBeanFactory.getBean(RedisIndexedSessionRepository.class);
        if (sessionRepository == null) {
            return;
        }

        // 根据用户名查找会话
        Map<String, ?> users = sessionRepository.findByPrincipalName(username);
        if (MapUtils.isNotEmpty(users)) {
            // 删除所有与该用户名关联的 session
            users.keySet().forEach(k -> {
                sessionRepository.deleteById(k);
                sessionRepository.getSessionRedisOperations().delete("spring:session:sessions:" + k);
            });
        }
    }

    /**
     * 权限验证时从 controller 参数列表中找到 organizationId 传入
     */
    public static void setCurrentOrganizationId(String organizationId) {
        SessionUtils.organizationId.set(organizationId);
    }

    /**
     * 权限验证时从 controller 参数列表中找到 ORGANIZATION
     */
    public static String getCurrentOrganizationId() {
        try {
            if (StringUtils.isNotEmpty(organizationId.get())) {
                return organizationId.get();
            }
            HttpServletRequest request = ((ServletRequestAttributes) (RequestContextHolder.currentRequestAttributes())).getRequest();
            LogUtils.debug("ORGANIZATION: {}", request.getHeader("ORGANIZATION"));
            if (request.getHeader("ORGANIZATION") != null) {
                return request.getHeader("ORGANIZATION");
            }
        } catch (Exception e) {
            LogUtils.error(e.getMessage(), e);
        }
        return Objects.requireNonNull(getUser()).getLastOrganizationId();
    }

    public static void clearCurrentOrganizationId() {
        organizationId.remove();
    }

    /**
     * 将当前用户信息保存到 Session 中。
     *
     * @param sessionUser 当前用户对象
     */
    public static void putUser(SessionUser sessionUser) {
        // 保存用户信息到 Session
        SecurityUtils.getSubject().getSession().setAttribute(ATTR_USER, sessionUser);
        // 保存用户 ID 到 Session
        SecurityUtils.getSubject().getSession().setAttribute(FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME, sessionUser.getId());
    }

    /**
     * 获取 HTTP 请求头的指定字段。
     *
     * @param headerName 请求头字段名称
     * @return 请求头字段的值，如果没有找到该字段或发生异常，则返回 null
     */
    public static String getHttpHeader(String headerName) {
        if (StringUtils.isBlank(headerName)) {
            return null;
        }
        try {
            HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
            return request.getHeader(headerName);
        } catch (Exception e) {
            return null;
        }
    }
}
