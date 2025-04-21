package io.cordys.crm.system.controller;

import io.cordys.common.constants.FormKey;
import io.cordys.common.constants.PermissionConstants;
import io.cordys.common.domain.BaseModuleFieldValue;
import io.cordys.common.pager.Pager;
import io.cordys.common.util.BeanUtils;
import io.cordys.crm.base.BaseTest;
import io.cordys.crm.system.domain.ModuleField;
import io.cordys.crm.system.domain.ModuleForm;
import io.cordys.crm.system.domain.Product;
import io.cordys.crm.system.domain.ProductField;
import io.cordys.crm.system.dto.request.ProductBatchEditRequest;
import io.cordys.crm.system.dto.request.ProductEditRequest;
import io.cordys.crm.system.dto.request.ProductPageRequest;
import io.cordys.crm.system.dto.response.product.ProductGetResponse;
import io.cordys.crm.system.dto.response.product.ProductListResponse;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.shaded.org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProductControllerTests extends BaseTest {
    private static final String BASE_PATH = "/product/";

    protected static final String MODULE_FORM = "module/form";

    private static Product addProduct;

    private static List<String> batchIds = new ArrayList<>();
    private static String moduleFieldPriceId = "";
    private static String moduleFieldStatusId=  "";

    @Resource
    private BaseMapper<Product> productBaseMapper;

    @Resource
    private BaseMapper<ProductField> productFieldBaseMapper;
    @Resource
    private BaseMapper<ModuleField> moduleFieldMapper;

    @Resource
    private BaseMapper<ModuleForm> moduleFormMapper;

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

        this.requestPostWithOk(DEFAULT_PAGE, request);

        // 校验权限
        requestPostPermissionTest(PermissionConstants.PRODUCT_MANAGEMENT_READ, DEFAULT_PAGE, request);
    }

    private ModuleForm getModuleForm() {
        ModuleForm example = new ModuleForm();
        example.setOrganizationId(DEFAULT_ORGANIZATION_ID);
        example.setFormKey(FormKey.PRODUCT.getKey());
        return moduleFormMapper.selectOne(example);
    }

    @Test
    @Order(2)
    void testAdd() throws Exception {
        // 请求成功
        ProductEditRequest request = new ProductEditRequest();
        request.setName("product");
        request.setPrice(BigDecimal.valueOf(7.23d));
        request.setStatus("1");
        MvcResult mvcResult = this.requestPostWithOkAndReturn(DEFAULT_ADD, request);
        Product resultData = getResultData(mvcResult, Product.class);
        // 校验请求成功数据
        addProduct = productBaseMapper.selectByPrimaryKey(resultData.getId());
        ModuleForm moduleForm = getModuleForm();

        ModuleField example = new ModuleField();
        example.setFormId(moduleForm.getId());
        List<ModuleField> select = moduleFieldMapper.select(example);
        ModuleField moduleFieldPrice= select
                .stream()
                .filter(field -> StringUtils.equals(field.getInternalKey(), "productPrice"))
                .findFirst().orElse(null);
        moduleFieldPriceId = moduleFieldPrice.getId();
        ModuleField moduleFieldStatus= select
                .stream()
                .filter(field -> StringUtils.equals(field.getInternalKey(), "productStatus"))
                .findFirst().orElse(null);
        moduleFieldStatusId = moduleFieldStatus.getId();
        request = new ProductEditRequest();
        request.setName("productOne");
        request.setPrice(BigDecimal.valueOf(7.23d));
        request.setStatus("1");

        request.setModuleFields(List.of(new BaseModuleFieldValue(moduleFieldPrice.getId(), 12),new BaseModuleFieldValue(moduleFieldStatus.getId(), "1")));

        mvcResult = this.requestPostWithOkAndReturn(DEFAULT_ADD, request);
        resultData = getResultData(mvcResult, Product.class);
        // 校验请求成功数据
        Product product = productBaseMapper.selectByPrimaryKey(resultData.getId());
        batchIds.add(product.getId());

        request = new ProductEditRequest();
        request.setName("productTwo");
        request.setPrice(BigDecimal.valueOf(7.23d));
        request.setStatus("1");
        request.setModuleFields(List.of(new BaseModuleFieldValue(moduleFieldPrice.getId(), 14),new BaseModuleFieldValue(moduleFieldStatus.getId(), "1")));
        mvcResult = this.requestPostWithOkAndReturn(DEFAULT_ADD, request);
        resultData = getResultData(mvcResult, Product.class);

        // 校验请求成功数据
        product = productBaseMapper.selectByPrimaryKey(resultData.getId());
        batchIds.add(product.getId());
        request = new ProductEditRequest();
        request.setName("productThree");
        request.setPrice(BigDecimal.valueOf(7.23d));
        request.setStatus("1");
        request.setModuleFields(List.of(new BaseModuleFieldValue(moduleFieldPrice.getId(), 13),new BaseModuleFieldValue(moduleFieldStatus.getId(), "1")));
        mvcResult = this.requestPostWithOkAndReturn(DEFAULT_ADD, request);
        resultData = getResultData(mvcResult, Product.class);
        // 校验请求成功数据
        product = productBaseMapper.selectByPrimaryKey(resultData.getId());
        batchIds.add(product.getId());
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
        request.setPrice(BigDecimal.valueOf(7.23d));
        request.setStatus("2");
        this.requestPostWithOk(DEFAULT_UPDATE, request);
        Product product = productBaseMapper.selectByPrimaryKey(addProduct.getId());
        Assertions.assertTrue(StringUtils.equalsIgnoreCase(product.getStatus(),"2"));
        // 校验权限
        requestPostPermissionTest(PermissionConstants.PRODUCT_MANAGEMENT_UPDATE, DEFAULT_UPDATE, request);

        request.setId(null);
        this.requestPost(DEFAULT_UPDATE, request).andExpect(status().is5xxServerError());
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
    @Order(6)
    void testBatchUpdate() throws Exception {
        // 请求成功
        ProductBatchEditRequest request = new ProductBatchEditRequest();
        request.setIds(batchIds);
        request.setPrice(BigDecimal.valueOf(3.00d));
        List<BaseModuleFieldValue> baseModuleFieldValues = List.of(new BaseModuleFieldValue(moduleFieldPriceId, 15), new BaseModuleFieldValue(moduleFieldStatusId, "2"));
        request.setModuleFields(baseModuleFieldValues);
        this.requestPostWithOk("batch/update", request);
        MvcResult mvcResult = this.requestGetWithOkAndReturn(DEFAULT_GET, batchIds.getFirst());
        ProductGetResponse getResponse = getResultData(mvcResult, ProductGetResponse.class);
        Assertions.assertEquals(BigDecimal.valueOf(300,2), getResponse.getPrice());
        for (BaseModuleFieldValue moduleField : getResponse.getModuleFields()) {
            if (StringUtils.equalsAnyIgnoreCase(moduleField.getFieldId(), moduleFieldStatusId)) {
                Assertions.assertEquals("1",moduleField.getFieldValue());
            }
        }
        // 校验权限
        requestPostPermissionTest(PermissionConstants.PRODUCT_MANAGEMENT_UPDATE, DEFAULT_UPDATE, request);


    }


    @Test
    @Order(7)
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

    @Test
    @Order(8)
    void batchDelete() throws Exception {

        this.requestPostWithOk("batch/delete", batchIds);
        Assertions.assertNull(productBaseMapper.selectByPrimaryKey(batchIds.getFirst()));

        ProductField example = new ProductField();
        example.setResourceId(batchIds.getFirst());
        List<ProductField> fields = productFieldBaseMapper.select(example);
        Assumptions.assumeTrue(CollectionUtils.isEmpty(fields));

        // 校验权限
        requestGetPermissionTest(PermissionConstants.PRODUCT_MANAGEMENT_DELETE, DEFAULT_DELETE, addProduct.getId());
    }
}