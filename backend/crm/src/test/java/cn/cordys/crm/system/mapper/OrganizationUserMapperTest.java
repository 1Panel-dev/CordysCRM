package cn.cordys.crm.system.mapper;

import cn.cordys.common.mapper.CommonMapper;
import cn.cordys.crm.system.dto.request.UserPageRequest;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrganizationUserMapperTest {

    private static final Configuration CONFIGURATION = new Configuration();

    @BeforeAll
    static void loadMappers() throws Exception {
        for (Class<?> mapper : List.of(CommonMapper.class, ExtOrganizationUserMapper.class, ExtUserMapper.class)) {
            String resource = mapper.getName().replace('.', '/') + ".xml";
            try (InputStream input = mapper.getClassLoader().getResourceAsStream(resource)) {
                assertNotNull(input, "找不到 Mapper XML：" + resource);
                new XMLMapperBuilder(input, CONFIGURATION, resource, CONFIGURATION.getSqlFragments()).parse();
            }
        }
    }

    @Test
    void listAlwaysFiltersByOrganization() {
        UserPageRequest request = new UserPageRequest();
        Map<String, Object> parameters = Map.of("request", request, "orgId", "org-a", "orderByClause", "1");

        assertTrue(listSql(parameters).contains("where sou.organization_id = ? order by"));
        request.setDepartmentIds(List.of());
        assertTrue(listSql(parameters).contains("where sou.organization_id = ? order by"));
        request.setDepartmentIds(List.of("9"));
        assertTrue(listSql(parameters).contains("where sou.organization_id = ? and sou.department_id in ( ? )"));
    }

    @Test
    void optionsBindNameKeywordAndRespectIncludeDisabled() {
        var statement = CONFIGURATION.getMappedStatement(ExtUserMapper.class.getName() + ".selectUserOptionByOrgId");
        BoundSql enabledOnly = statement.getBoundSql(Map.of("orgId", "org-a", "defaultOrder", "",
                "includeDisabled", false));
        assertTrue(enabledOnly.getSql().contains("and sou.enable = true"));
        assertFalse(enabledOnly.getSql().contains("su.name like"));

        BoundSql filtered = statement.getBoundSql(Map.of("orgId", "org-a", "defaultOrder", "",
                "includeDisabled", true, "keyword", "员工"));
        assertFalse(filtered.getSql().contains("and sou.enable = true"));
        assertTrue(filtered.getSql().contains("and su.name like concat('%', ?, '%')"));
        assertEquals(List.of("orgId", "keyword"),
                filtered.getParameterMappings().stream().map(mapping -> mapping.getProperty()).toList());
    }

    private String listSql(Map<String, Object> parameters) {
        return CONFIGURATION.getMappedStatement(ExtOrganizationUserMapper.class.getName() + ".list")
                .getBoundSql(parameters).getSql().replaceAll("\\s+", " ");
    }
}
