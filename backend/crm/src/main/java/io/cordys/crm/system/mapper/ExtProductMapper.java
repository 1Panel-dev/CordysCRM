package io.cordys.crm.system.mapper;

import io.cordys.common.dto.OptionDTO;
import io.cordys.crm.system.domain.Product;
import io.cordys.crm.system.dto.response.product.ProductListResponse;
import io.cordys.crm.system.dto.request.ProductPageRequest;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 *
 * @author jianxing
 * @date 2025-02-08 17:42:41
 */
public interface ExtProductMapper {

    List<ProductListResponse> list(@Param("request") ProductPageRequest request, @Param("orgId") String orgId);

    boolean checkAddExist(@Param("product") Product product);

    boolean checkUpdateExist(@Param("product") Product product);

    List<Product> listByIds(@Param("ids") List<String> ids);

    void updateProduct(@Param("ids") List<String> ids, @Param("product") Product product);

    List<OptionDTO> getOptions(@Param("orgId") String orgId);
}
