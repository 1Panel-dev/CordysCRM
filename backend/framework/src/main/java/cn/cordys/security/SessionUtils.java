package cn.cordys.security;

import cn.cordys.common.util.CommonBeanFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.Strings;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.data.redis.RedisIndexedSessionRepository;

import java.util.Map;

import static cn.cordys.security.SessionConstants.ATTR_USER;


/**
 * Session 工具类，提供操作用户 Session 的常用方法。
 * <p>
 * 包含获取当前用户信息、获取 Session ID、踢除用户等功能。
 * </p>
 */
@Slf4j
public class SessionUtils {

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
            log.warn("后台获取在线用户失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 获取当前 Session 的 ID。
     *
     * @return 当前 Session 的 ID
     */
    public static String getSessionId() {
        try {
            return (String) SecurityUtils.getSubject().getSession().getId();
        } catch (Exception e) {
            return null;
        }
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
     * 踢除指定用户（从 Redis 会话中删除）。
     *
     * @param operatorId 操作用户 ID
     * @param kickUserId 被踢用户 ID
     */
    public static void kickOutUser(String operatorId, String kickUserId) {
        // 处理用户会话
        boolean isSelfReset = Strings.CS.equals(operatorId, kickUserId);
        if (isSelfReset) {
            // 当前用户重置自己的密码，直接登出
            SecurityUtils.getSubject().logout();
            // 需要检查是否有其他会话存在
            try {
                SessionUtils.kickOutUser(kickUserId);
            } catch (Exception e) {
                log.error("踢出用户失败: " + e.getMessage());
            }
        } else {
            // 管理员重置他人密码，踢出该用户
            SessionUtils.kickOutUser(kickUserId);
        }

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
}
