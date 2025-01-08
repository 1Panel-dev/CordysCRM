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

@OpenAPIDefinition(
        info = @Info(
                title = "${spring.application.name}",
                version = "3.0"
        ),
        servers = @Server(url = "/")
)
@Configuration
public class OpenApiConfig {
    private static final String prePackages = "io.cordys.crm.";

    @Bean
    @ConditionalOnProperty(name = {"springdoc.swagger-ui.enabled", "springdoc.api-docs.enabled"}, havingValue = "true")
    public OperationCustomizer customize() {
        return (operation, handlerMethod) -> {
            if (!"login".equals(handlerMethod.getMethod().getName())) {
                return operation
                        .addParametersItem(new Parameter().in("header").required(true).name(SessionConstants.CSRF_TOKEN))
                        .addParametersItem(new Parameter().in("header").required(true).name(SessionConstants.HEADER_TOKEN));
            }
            return operation;
        };
    }

    @Bean
    @ConditionalOnProperty(name = {"springdoc.swagger-ui.enabled", "springdoc.api-docs.enabled"}, havingValue = "true")
    public GroupedOpenApi systemApi() {
        return GroupedOpenApi.builder()
                .group("system")
                .packagesToScan(prePackages + "system")
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = {"springdoc.swagger-ui.enabled", "springdoc.api-docs.enabled"}, havingValue = "true")
    public GroupedOpenApi customerApi() {
        return GroupedOpenApi.builder()
                .group("customer")
                .packagesToScan(prePackages + "customer")
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = {"springdoc.swagger-ui.enabled", "springdoc.api-docs.enabled"}, havingValue = "true")
    public GroupedOpenApi leadApi() {
        return GroupedOpenApi.builder()
                .group("lead")
                .packagesToScan(prePackages + "lead")
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = {"springdoc.swagger-ui.enabled", "springdoc.api-docs.enabled"}, havingValue = "true")
    public GroupedOpenApi opportunityApi() {
        return GroupedOpenApi.builder()
                .group("opportunity")
                .packagesToScan(prePackages + "opportunity")
                .build();
    }

}
