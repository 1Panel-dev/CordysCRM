package cn.cordys.crm.integration.sso.service;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.integration.sso.constants.OAuthStateFlow;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * OAuth {@code state} 参数的签发与校验服务。
 *
 * <p>服务将高强度随机 {@code state} 与发起授权时的 HTTP Session 绑定，并在回调校验时将其一次性消费，
 * 用于阻止攻击者伪造登录回调、替换授权码或重放已使用的回调请求。</p>
 *
 * <p>待校验状态只在服务端 Session 中保存，具有有效期和数量上限，不依赖前端传回任何可信信息。</p>
 */
@Service
public class OAuthStateService {

    /** HTTP Session 中保存待校验 state 集合的属性名。 */
    private static final String OAUTH_STATES_SESSION_KEY = "cordys.oauth2.pending-states";

    /** state 的有效期为 10 分钟，覆盖正常授权过程并限制攻击窗口。 */
    private static final long STATE_TTL_MILLIS = 10 * 60 * 1000L;

    /** 单个 Session 允许并行发起的最大授权请求数，避免状态集合无界增长。 */
    private static final int MAX_PENDING_STATES = 10;

    /** 密码学安全随机数生成器，用于保证 state 不可预测。 */
    private final SecureRandom secureRandom;

    /** 提供当前时间；抽象为 Clock 便于稳定测试过期逻辑。 */
    private final Clock clock;

    /** 使用系统安全随机源和 UTC 时钟创建服务。 */
    public OAuthStateService() {
        this(new SecureRandom(), Clock.systemUTC());
    }

    /** 测试专用构造方法，允许注入可控的随机源与时钟。 */
    OAuthStateService(SecureRandom secureRandom, Clock clock) {
        this.secureRandom = secureRandom;
        this.clock = clock;
    }

    /**
     * 根据外部流程标识签发 state。
     *
     * <p>该重载负责先将不可信字符串转换为受支持的枚举，防止任意流程名称进入 state。</p>
     *
     * @param flowValue OAuth 流程标识
     * @param session 发起授权请求的 HTTP Session
     * @return 可用于 OAuth 授权请求的 state
     * @throws GenericException 流程标识不受支持时抛出
     */
    public String generateState(String flowValue, HttpSession session) {
        OAuthStateFlow flow = OAuthStateFlow.fromValue(flowValue).orElseThrow(this::invalidState);
        return generateState(flow, session);
    }

    /**
     * 为指定 OAuth 流程生成并保存一次性 state。
     *
     * <p>state 由流程前缀和 256 位安全随机值组成。写入前会清理过期数据，并在达到容量上限时移除最早过期的记录。
     * 对 Session 加锁可避免同一浏览器并行发起多个登录请求时发生更新覆盖。</p>
     *
     * @param flow OAuth 流程
     * @param session 发起授权请求的 HTTP Session
     * @return 新生成的 state
     */
    public String generateState(OAuthStateFlow flow, HttpSession session) {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String state = flow.getValue() + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        long now = clock.millis();

        synchronized (session) {
            Map<String, Long> states = getStates(session);
            states.entrySet().removeIf(entry -> entry.getValue() < now);
            if (states.size() >= MAX_PENDING_STATES) {
                String oldest = states.entrySet().stream()
                        .min(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse(null);
                states.remove(oldest);
            }
            states.put(state, now + STATE_TTL_MILLIS);
            session.setAttribute(OAUTH_STATES_SESSION_KEY, states);
        }
        return state;
    }

    /**
     * 校验并消费 OAuth 回调中的 state。
     *
     * <p>校验内容包括参数非空、流程前缀匹配、存在于当前 Session 且未过期。无论记录是否过期，读取后都会先从
     * Session 中删除，从而确保同一个 state 最多成功使用一次，后续重放请求会被拒绝。</p>
     *
     * @param flow 当前回调对应的 OAuth 流程
     * @param state OAuth 服务商原样返回的 state
     * @param session 当前回调恢复出的 HTTP Session
     * @throws GenericException state 缺失、流程不匹配、不存在、已过期或已被消费时抛出
     */
    public void validateAndConsume(OAuthStateFlow flow, String state, HttpSession session) {
        if (StringUtils.isBlank(state) || !state.startsWith(flow.getValue() + ".")) {
            throw invalidState();
        }

        synchronized (session) {
            Map<String, Long> states = getStates(session);
            Long expiresAt = states.remove(state);
            session.setAttribute(OAUTH_STATES_SESSION_KEY, states);
            if (expiresAt == null || expiresAt < clock.millis()) {
                throw invalidState();
            }
        }
    }

    /**
     * 获取待校验 state 的可修改副本。
     *
     * <p>不直接暴露 Session 内的 Map，避免校验过程中的临时修改绕过统一的 Session 回写。</p>
     */
    @SuppressWarnings("unchecked")
    private Map<String, Long> getStates(HttpSession session) {
        Object value = session.getAttribute(OAUTH_STATES_SESSION_KEY);
        if (value instanceof Map<?, ?>) {
            return new HashMap<>((Map<String, Long>) value);
        }
        return new HashMap<>();
    }

    private GenericException invalidState() {
        return new GenericException(Translator.get("oauth.state.invalid"));
    }
}
