package cn.cordys.crm.product.mapper;

import cn.cordys.common.dto.BatchUpdateDbParam;
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
     *
     * @param request    请求参数
     * @param currentOrg 当前组织
     * @return 价格列表
     */
    List<ProductPriceResponse> list(@Param("request") ProductPricePageRequest request, @Param("currentOrg") String currentOrg);

	/**
	 * 获取位置值
	 * @param orgId 组织ID
	 * @return 位置值
	 */
	Long getPos(@Param("orgId") String orgId);

	/**
	 * 获取前置值
	 * @param orgId 组织ID
	 * @param basePos 基础值
	 * @param userId 用户ID
	 * @param resourceType 资源类型
	 * @return 位置值
	 */
    Long getPrePos(@Param("orgId") String orgId, @Param("basePos") Long basePos, @Param("userId") String userId, @Param("resourceType") String resourceType);

	/**
	 * 获取后置位置值
	 * @param orgId 组织ID
	 * @param basePos 基础值
	 * @param userId 用户ID
	 * @param resourceType 资源类型
	 * @return 位置值
	 */
    Long getLastPos(@Param("orgId") String orgId, @Param("basePos") Long basePos, @Param("userId") String userId, @Param("resourceType") String resourceType);

	/**
	 * 批量更新
	 * @param request 请求参数
	 */
	void batchUpdate(@Param("request") BatchUpdateDbParam request);
}
