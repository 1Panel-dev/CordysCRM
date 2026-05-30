package cn.cordys.crm.clue.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClueFollowDTO {

	/**
	 * 最新跟进人
	 */
	private String follower;
	/**
	 * 最新跟进时间
	 */
	private Long followerTime;
}
