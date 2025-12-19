package cn.cordys.crm.system.controller;

import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.dto.request.PosRequest;
import cn.cordys.common.pager.Pager;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.crm.base.BaseTest;
import cn.cordys.crm.system.domain.ModuleField;
import cn.cordys.crm.system.domain.ModuleForm;
import cn.cordys.crm.product.domain.Product;
import cn.cordys.crm.product.domain.ProductField;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.product.dto.request.ProductEditRequest;
import cn.cordys.crm.product.dto.request.ProductPageRequest;
import cn.cordys.crm.system.dto.request.ResourceBatchEditRequest;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.product.dto.response.ProductGetResponse;
import cn.cordys.crm.product.dto.response.ProductListResponse;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Strings;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProductControllerTests extends BaseTest {
    protected static final String MODULE_FORM = "module/form";
    protected static final String BATCH_UPDATE = "batch/update";
    private static final String BASE_PATH = "/product/";
    private static final List<String> batchIds = new ArrayList<>();
    private static Product addProduct;
    private static ModuleFormConfigDTO moduleFormConfig;

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
        MvcResult mvcResult = this.requestGetWithOkAndReturn(MODULE_FORM);
        moduleFormConfig = getResultData(mvcResult, ModuleFormConfigDTO.class);
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
        ModuleField moduleFieldPrice = select
                .stream()
                .filter(field -> Strings.CS.equals(field.getInternalKey(), "productPrice"))
                .findFirst().orElse(null);
        ModuleField moduleFieldStatus = select
                .stream()
                .filter(field -> Strings.CS.equals(field.getInternalKey(), "productStatus"))
                .findFirst().orElse(null);
        request = new ProductEditRequest();
        request.setName("productOne");
        request.setPrice(BigDecimal.valueOf(7.23d));
        request.setStatus("1");

        request.setModuleFields(List.of(new BaseModuleFieldValue(moduleFieldPrice.getId(), 12), new BaseModuleFieldValue(moduleFieldStatus.getId(), "1")));

        mvcResult = this.requestPostWithOkAndReturn(DEFAULT_ADD, request);
        resultData = getResultData(mvcResult, Product.class);
        // 校验请求成功数据
        Product product = productBaseMapper.selectByPrimaryKey(resultData.getId());
        batchIds.add(product.getId());

        request = new ProductEditRequest();
        request.setName("productTwo");
        request.setPrice(BigDecimal.valueOf(7.23d));
        request.setStatus("1");
        request.setModuleFields(List.of(new BaseModuleFieldValue(moduleFieldPrice.getId(), 14), new BaseModuleFieldValue(moduleFieldStatus.getId(), "1")));
        mvcResult = this.requestPostWithOkAndReturn(DEFAULT_ADD, request);
        resultData = getResultData(mvcResult, Product.class);

        // 校验请求成功数据
        product = productBaseMapper.selectByPrimaryKey(resultData.getId());
        batchIds.add(product.getId());
        request = new ProductEditRequest();
        request.setName("productThree");
        request.setPrice(BigDecimal.valueOf(7.23d));
        request.setStatus("1");
        request.setModuleFields(List.of(new BaseModuleFieldValue(moduleFieldPrice.getId(), 13), new BaseModuleFieldValue(moduleFieldStatus.getId(), "1")));
        mvcResult = this.requestPostWithOkAndReturn(DEFAULT_ADD, request);
        resultData = getResultData(mvcResult, Product.class);
        // 校验请求成功数据
        product = productBaseMapper.selectByPrimaryKey(resultData.getId());
        batchIds.add(product.getId());
        // 校验权限
        requestPostPermissionTest(PermissionConstants.PRODUCT_MANAGEMENT_ADD, DEFAULT_ADD, request);

        request = new ProductEditRequest();
        request.setName("productFour");
        request.setPrice(BigDecimal.valueOf(100000000000d));
        request.setStatus("1");
        request.setModuleFields(List.of(new BaseModuleFieldValue(moduleFieldPrice.getId(), 13), new BaseModuleFieldValue(moduleFieldStatus.getId(), "1")));
        this.requestPost(DEFAULT_ADD, request).andExpect(status().is4xxClientError());

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
        Assertions.assertTrue(Strings.CI.equals(product.getStatus(), "2"));
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
        responseProduct.setPos(4096L);
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
            responseProduct.setPos(product.getPos());
            Assertions.assertEquals(product, responseProduct);
        });


        // 校验权限
        requestPostPermissionTest(PermissionConstants.PRODUCT_MANAGEMENT_READ, DEFAULT_PAGE, request);
    }

    @Test
    @Order(6)
    void testBatchUpdate() throws Exception {
        ResourceBatchEditRequest request = new ResourceBatchEditRequest();
        for (BaseField field : moduleFormConfig.getFields()) {
            request.setIds(List.of(addProduct.getId()));
            request.setFieldId(field.getId());
            if (Strings.CS.equals(field.getName(), BusinessModuleField.PRODUCT_PRICE.name())) {
                request.setFieldValue(3.00d);
                this.requestPostWithOk(BATCH_UPDATE, request);
            } else if (Strings.CS.equals(field.getName(), BusinessModuleField.PRODUCT_NAME.name())) {
                request.setFieldValue(UUID.randomUUID().toString());
                this.requestPostWithOk(BATCH_UPDATE, request);
            }
        }
        // 校验权限
        requestPostPermissionTest(PermissionConstants.PRODUCT_MANAGEMENT_UPDATE, BATCH_UPDATE, request);
    }

    @Test
    @Order(7)
    void post() throws Exception {
        PosRequest request = new PosRequest();
        request.setOrgId(DEFAULT_ORGANIZATION_ID);
        request.setMoveId(addProduct.getId());
        System.out.println(addProduct.getPos());
        request.setMoveMode("after");
        request.setTargetId(batchIds.getFirst());
        this.requestPostWithOk("/edit/pos", request);
        Product product = productBaseMapper.selectByPrimaryKey(addProduct.getId());
        System.out.println(product.getPos());
        Assertions.assertTrue(addProduct.getPos() < product.getPos());
    }


    @Test
    @Order(8)
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
    @Order(9)
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

    @Test
    @Order(10)
    void getOption() throws Exception {

        MvcResult mvcResult = this.requestGetWithOkAndReturn("list/option");
        List<OptionDTO> optionDTOList = getResultDataArray(mvcResult, OptionDTO.class);
        Assertions.assertNotNull(optionDTOList);

        // 校验权限
        requestGetPermissionTest(PermissionConstants.PRODUCT_MANAGEMENT_READ, DEFAULT_GET, addProduct.getId());
    }
}