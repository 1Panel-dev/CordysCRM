package cn.cordys.crm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.quartz.autoconfigure.QuartzAutoConfiguration;
import org.springframework.boot.web.server.servlet.context.ServletComponentScan;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(exclude = QuartzAutoConfiguration.class, excludeName = {
        "org.springframework.boot.ldap.autoconfigure.LdapAutoConfiguration",
        "org.springframework.boot.neo4j.autoconfigure.Neo4jAutoConfiguration"
})
@ServletComponentScan
@ComponentScan(basePackages = { "cn.cordys" })
public class TestApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
}
