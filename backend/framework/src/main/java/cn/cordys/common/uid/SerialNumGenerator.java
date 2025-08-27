package cn.cordys.common.uid;

import cn.cordys.common.exception.GenericException;
import cn.cordys.common.util.LogUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Component
public class SerialNumGenerator {

	private final StringRedisTemplate stringRedisTemplate;

	private static final int MAX_RULES_SIZE = 5;

	public SerialNumGenerator(StringRedisTemplate stringRedisTemplate) {
		this.stringRedisTemplate = stringRedisTemplate;
	}

	/**
	 * 按照流水号配置规则生成
	 * @param rules 配置规则
	 * @return 流水号
	 */
	public String generateByRules(List<String> rules, String orgId, String formKey) {
		if (CollectionUtils.isEmpty(rules) || rules.size() < MAX_RULES_SIZE) {
			throw new GenericException("流水号规则配置有误");
		}
		String date = new SimpleDateFormat(rules.get(2)).format(new Date());
		String redisKey = "serial:" + orgId + ":" + formKey + ":" + date;
		Long seq;
		try {
			seq = stringRedisTemplate.opsForValue().increment(redisKey);
			return String.format("%s%s%s%s%0" + rules.getLast() + "d", rules.get(0), rules.get(1), date, rules.get(3), seq);
		} catch (Exception e) {
			LogUtils.error("获取流水号失败: " + e.getMessage());
		}
		return null;
	}
}
