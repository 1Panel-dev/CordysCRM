package cn.cordys.common.pager;

import cn.cordys.common.dto.OptionDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class PagerWithOption<T> extends Pager<T> {

    /**
     * 选项集合
     */
    @Schema(description = "选项集合")
    private Map<String, List<OptionDTO>> optionMap;
}
