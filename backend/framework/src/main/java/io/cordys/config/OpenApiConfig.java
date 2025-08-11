package io.cordys.config;

import io.cordys.security.SessionConstants;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 配置类，配置 Swagger UI 和 API 文档生成。
 */
@OpenAPIDefinition(
        info = @Info(
                title = "${spring.application.name}",
                version = "1.0.0"
        ),
        servers = @Server(url = "/")
)
@Configuration
@ConditionalOnProperty(name = {"springdoc.swagger-ui.enabled", "springdoc.api-docs.enabled"}, havingValue = "true")
public class OpenApiConfig {

    private static final String PRE_PACKAGES = "io.cordys.crm.";

    /**
     * 自定义 API 操作，增加 CSRF 和 HEADER token 参数。
     */
    @Bean
    public OperationCustomizer customize() {
        return (operation, handlerMethod) -> {
            // 排除登录接口，不添加 CSRF 和 HEADER token 参数
            if (!"login".equals(handlerMethod.getMethod().getName())) {
                return operation
                        .addParametersItem(new Parameter().in("header").required(true).name(SessionConstants.CSRF_TOKEN))
                        .addParametersItem(new Parameter().in("header").required(true).name(SessionConstants.HEADER_TOKEN));
            }
            return operation;
        };
    }

    /**
     * 根据模块名称创建 GroupedOpenApi 配置。
     *
     * @param group         API 分组名称
     * @param packagePrefix 扫描的包前缀
     * @return GroupedOpenApi
     */
    private GroupedOpenApi createApi(String group, String packagePrefix) {
        return GroupedOpenApi.builder()
                .group(group)
                .packagesToScan(packagePrefix)
                .build();
    }

    /**
     * 配置系统 API 文档。
     */
    @Bean
    public GroupedOpenApi systemApi() {
        return createApi("system", PRE_PACKAGES + "system");
    }

    /**
     * 配置客户 API 文档。
     */
    @Bean
    public GroupedOpenApi customerApi() {
        return createApi("customer", PRE_PACKAGES + "customer");
    }

    /**
     * 配置潜在客户 API 文档。
     */
    @Bean
    public GroupedOpenApi clueApi() {
        return createApi("clue", PRE_PACKAGES + "clue");
    }

    /**
     * 配置机会 API 文档。
     */
    @Bean
    public GroupedOpenApi opportunityApi() {
        return createApi("opportunity", PRE_PACKAGES + "opportunity");
    }

    /**
     * 配置首页 API 文档。
     */
    @Bean
    public GroupedOpenApi homeApi() {
        return createApi("home", PRE_PACKAGES + "home");
    }

    /**
     * 配置全局搜索 API 文档
     */
    @Bean
    public GroupedOpenApi searchApi() {
        return createApi("search", PRE_PACKAGES + "search");
    }

    /**
     * 配置 XPack API 文档。
     */
    @Bean
    public GroupedOpenApi xpackApi() {
        return createApi("xpack", "io.cordys.xpack");
    }
}
