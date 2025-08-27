package cn.cordys.crm.system.dto.request;


import cn.cordys.common.dto.condition.CombineSearch;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserViewAddRequest extends CombineSearch {

    @Schema(description = "视图名称")
    @NotBlank
    private String name;

}
