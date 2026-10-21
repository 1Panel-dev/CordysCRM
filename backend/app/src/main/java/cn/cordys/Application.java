package cn.cordys;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.quartz.autoconfigure.QuartzAutoConfiguration;
import org.springframework.boot.web.server.servlet.context.ServletComponentScan;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication(exclude = QuartzAutoConfiguration.class, excludeName = {
        "org.springframework.boot.ldap.autoconfigure.LdapAutoConfiguration",
        "org.springframework.boot.neo4j.autoconfigure.Neo4jAutoConfiguration"
})
@PropertySource(value = {
        "classpath:commons.properties",
        "file:/opt/cordys/conf/cordys-crm.properties",
}, encoding = "UTF-8", ignoreResourceNotFound = true)
@ServletComponentScan
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
