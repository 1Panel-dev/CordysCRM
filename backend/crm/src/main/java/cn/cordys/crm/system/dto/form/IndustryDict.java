package cn.cordys.crm.system.dto.form;

import lombok.Data;

import java.util.List;

/**
 * @author song-cc-rock
 */
@Data
public class IndustryDict {

	private String label;
	private String value;
	private List<IndustryDict> children;
}
