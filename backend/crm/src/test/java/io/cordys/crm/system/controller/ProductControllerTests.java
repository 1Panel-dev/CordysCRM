package io.cordys.crm.system.controller;

import io.cordys.common.constants.PermissionConstants;
import io.cordys.common.pager.Pager;
import io.cordys.common.util.BeanUtils;
import io.cordys.crm.base.BaseTest;
import io.cordys.crm.system.domain.Product;
import io.cordys.crm.system.domain.ProductField;
import io.cordys.crm.system.dto.request.ProductEditRequest;
import io.cordys.crm.system.dto.request.ProductPageRequest;
import io.cordys.crm.system.dto.response.ProductGetResponse;
import io.cordys.crm.system.dto.response.ProductListResponse;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProductControllerTests extends BaseTest {
    private static final String BASE_PATH = "/product/";

    protected static final String MODULE_FORM = "module/form";

    private static Product addProduct;

    @Resource
    private BaseMapper<Product> productBaseMapper;

    @Resource
    private BaseMapper<ProductField> productFieldBaseMapper;

    @Override
    protected String getBasePath() {
        return BASE_PATH;
    }

    @Test
    @Order(0)
    void testModuleField() throws Exception {
        this.requestGetWithOkAndReturn(MODULE_FORM);

        // 校验权限
        requestGetPermissionTest(PermissionConstants.PRODUCT_MANAGEMENT_READ, MODULE_FORM);
    }

    @Test
    @Order(1)
    void testPageEmpty() throws Exception {
        ProductPageRequest request = new ProductPageRequest();
        request.setCurrent(1);
        request.setPageSize(10);

        MvcResult mvcResult = this.requestPostWithOkAndReturn(DEFAULT_PAGE, request);
        Pager<List<ProductListResponse>> pageResult = getPageResult(mvcResult, ProductListResponse.class);
        List<ProductListResponse> productListResponses = pageResult.getList();

        // 校验权限
        requestPostPermissionTest(PermissionConstants.PRODUCT_MANAGEMENT_READ, DEFAULT_PAGE, request);
    }

    @Test
    @Order(2)
    void testAdd() throws Exception {
        // 请求成功
        ProductEditRequest request = new ProductEditRequest();
        request.setName("product");
        MvcResult mvcResult = this.requestPostWithOkAndReturn(DEFAULT_ADD, request);
        Product resultData = getResultData(mvcResult, Product.class);
        // 校验请求成功数据
        addProduct = productBaseMapper.selectByPrimaryKey(resultData.getId());

        // 校验权限
        requestPostPermissionTest(PermissionConstants.PRODUCT_MANAGEMENT_ADD, DEFAULT_ADD, request);
    }

    @Test
    @Order(3)
    void testUpdate() throws Exception {
        // 请求成功
        ProductEditRequest request = new ProductEditRequest();
        request.setName("product");
        request.setId(addProduct.getId());
        this.requestPostWithOk(DEFAULT_UPDATE, request);
        // 校验权限
        requestPostPermissionTest(PermissionConstants.PRODUCT_MANAGEMENT_UPDATE, DEFAULT_UPDATE, request);
    }

    @Test
    @Order(4)
    void testGet() throws Exception {
        MvcResult mvcResult = this.requestGetWithOkAndReturn(DEFAULT_GET, addProduct.getId());
        ProductGetResponse getResponse = getResultData(mvcResult, ProductGetResponse.class);

        // 校验请求成功数据
        Product product = productBaseMapper.selectByPrimaryKey(addProduct.getId());
        Product responseProduct = BeanUtils.copyBean(new Product(), getResponse);
        responseProduct.setOrganizationId(DEFAULT_ORGANIZATION_ID);
        Assertions.assertEquals(responseProduct, product);
        // 校验权限
        requestGetPermissionTest(PermissionConstants.PRODUCT_MANAGEMENT_READ, DEFAULT_GET, addProduct.getId());
    }


    @Test
    @Order(5)
    void testPage() throws Exception {
        ProductPageRequest request = new ProductPageRequest();
        request.setCurrent(1);
        request.setPageSize(10);

        MvcResult mvcResult = this.requestPostWithOkAndReturn(DEFAULT_PAGE, request);
        Pager<List<ProductListResponse>> pageResult = getPageResult(mvcResult, ProductListResponse.class);
        List<ProductListResponse> resultList = pageResult.getList();

        Product example = new Product();
        example.setOrganizationId(DEFAULT_ORGANIZATION_ID);
        Map<String, Product> productMap = productBaseMapper.select(example)
                .stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        resultList.forEach(productListResponse -> {
            Product product = productMap.get(productListResponse.getId());
            Product responseProduct = BeanUtils.copyBean(new Product(), productListResponse);
            responseProduct.setOrganizationId(DEFAULT_ORGANIZATION_ID);
            Assertions.assertEquals(product, responseProduct);
        });


        // 校验权限
        requestPostPermissionTest(PermissionConstants.PRODUCT_MANAGEMENT_READ, DEFAULT_PAGE, request);
    }

    @Test
    @Order(10)
    void delete() throws Exception {
        this.requestGetWithOk(DEFAULT_DELETE, addProduct.getId());
        Assertions.assertNull(productBaseMapper.selectByPrimaryKey(addProduct.getId()));

        ProductField example = new ProductField();
        example.setResourceId(addProduct.getId());
        List<ProductField> fields = productFieldBaseMapper.select(example);
        Assumptions.assumeTrue(CollectionUtils.isEmpty(fields));

        // 校验权限
        requestGetPermissionTest(PermissionConstants.PRODUCT_MANAGEMENT_DELETE, DEFAULT_DELETE, addProduct.getId());
    }
}