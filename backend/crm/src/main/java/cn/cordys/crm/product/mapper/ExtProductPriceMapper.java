package cn.cordys.crm.product.mapper;

import cn.cordys.crm.product.dto.request.ProductPricePageRequest;
import cn.cordys.crm.product.dto.response.ProductPriceResponse;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author song-cc-rock
 */
public interface ExtProductPriceMapper {

	/**
	 * 查询价格列表
	 * @param request 请求参数
	 * @param currentOrg 当前组织
	 * @return 价格列表
	 */
    List<ProductPriceResponse> list(@Param("request") ProductPricePageRequest request, @Param("currentOrg") String currentOrg);
}
