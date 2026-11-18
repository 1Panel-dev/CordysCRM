package cn.cordys.crm.clue.dto.response;

import cn.cordys.common.pager.PagerWithOption;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.List;

@Getter
public class CluePageResponse extends PagerWithOption<List<ClueListResponse>> {

    @Schema(description = "当前视图筛选结果中是否存在线索审批中")
    private final boolean existApprovingData;

    public CluePageResponse(PagerWithOption<List<ClueListResponse>> page, boolean existApprovingData) {
        setList(page.getList());
        setTotal(page.getTotal());
        setPageSize(page.getPageSize());
        setCurrent(page.getCurrent());
        setOptionMap(page.getOptionMap());
        this.existApprovingData = existApprovingData;
    }
}
