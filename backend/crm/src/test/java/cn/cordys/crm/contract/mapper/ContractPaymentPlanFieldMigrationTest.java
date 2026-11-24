package cn.cordys.crm.contract.mapper;

import cn.cordys.crm.TestApplication;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptStatementFailedException;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("integration")
@SpringBootTest(classes = TestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class ContractPaymentPlanFieldMigrationTest {
    private static final String MIGRATION_PATH = "migration/1.10.0/";
    private static final String BACKFILL = "dml/V1.10.0_11__backfill_contract_payment_plan_fields.sql";
    private static final String DROP_LEGACY = "ddl/V1.10.0_12__drop_legacy_contract_payment_plan_columns.sql";
    private static final List<String> TABLES = List.of(
            "sys_module_form", "sys_module_field", "contract_payment_plan", "contract_payment_plan_field");

    @Resource
    private JdbcTemplate jdbc;

    @Test
    void backfillsLegacyValuesBeforeDroppingColumns() throws IOException {
        try {
            for (String table : TABLES) {
                jdbc.execute("CREATE TABLE ppmig_" + table + " LIKE " + table);
            }
            jdbc.execute("""
                    ALTER TABLE ppmig_contract_payment_plan
                        ADD COLUMN plan_amount DECIMAL(20, 10),
                        ADD COLUMN plan_end_time BIGINT,
                        ADD INDEX idx_plan_end_time (plan_end_time)
                    """);
            seedLegacyPlans();

            assertThrows(ScriptStatementFailedException.class, () -> executeMigration(DROP_LEGACY));
            assertEquals(2, legacyColumnCount());
            var expected = List.of(
                    List.of("ppmig-plan-a", "ppmig-a-amount", "0.0000000000"),
                    List.of("ppmig-plan-a", "ppmig-a-date", "1700000000123"),
                    List.of("ppmig-plan-b", "ppmig-b-amount", "1234.5678901234"),
                    List.of("ppmig-plan-c", "ppmig-b-date", "1700000000789"));
            for (int attempt = 0; attempt < 2; attempt++) {
                executeMigration(BACKFILL);
                assertEquals(expected, fieldValues());
            }
            executeMigration(DROP_LEGACY);
            assertEquals(0, legacyColumnCount());
            assertEquals(expected, fieldValues());
        } finally {
            for (String table : TABLES.reversed()) {
                jdbc.execute("DROP TABLE IF EXISTS ppmig_" + table);
            }
        }
    }

    private void seedLegacyPlans() {
        jdbc.update("""
                INSERT INTO ppmig_sys_module_form (id, form_key, organization_id, create_time, update_time,
                                             create_user, update_user)
                VALUES ('ppmig-form-a', 'contractPaymentPlan', 'ppmig-org-a', 0, 0, 'admin', 'admin'),
                       ('ppmig-form-b', 'contractPaymentPlan', 'ppmig-org-b', 0, 0, 'admin', 'admin')
                """);
        jdbc.update("""
                INSERT INTO ppmig_sys_module_field (id, form_id, internal_key, name, type, pos,
                                              create_user, create_time, update_user, update_time)
                VALUES ('ppmig-a-amount', 'ppmig-form-a', 'contractPaymentPlanPlanAmount',
                        '计划回款金额', 'INPUT_NUMBER', 0, 'admin', 0, 'admin', 0),
                       ('ppmig-a-date', 'ppmig-form-a', 'contractPaymentPlanPlanEndTime',
                        '计划回款时间', 'DATE_TIME', 1, 'admin', 0, 'admin', 0),
                       ('ppmig-b-amount', 'ppmig-form-b', 'contractPaymentPlanPlanAmount',
                        '计划回款金额', 'INPUT_NUMBER', 0, 'admin', 0, 'admin', 0),
                       ('ppmig-b-date', 'ppmig-form-b', 'contractPaymentPlanPlanEndTime',
                        '计划回款时间', 'DATE_TIME', 1, 'admin', 0, 'admin', 0)
                """);
        jdbc.update("""
                INSERT INTO ppmig_contract_payment_plan (id, name, contract_id, owner, plan_status, plan_amount,
                                                   plan_end_time, organization_id, create_time, update_time,
                                                   create_user, update_user)
                VALUES ('ppmig-plan-a', '回款计划一', 'ppmig-contract', 'admin', 'PENDING',
                        0, 1700000000123, 'ppmig-org-a', 0, 0, 'admin', 'admin'),
                       ('ppmig-plan-b', '回款计划二', 'ppmig-contract', 'admin', 'PENDING',
                        1234.5678901234, NULL, 'ppmig-org-b', 0, 0, 'admin', 'admin'),
                       ('ppmig-plan-c', '回款计划三', 'ppmig-contract', 'admin', 'PENDING',
                        NULL, 1700000000456, 'ppmig-org-b', 0, 0, 'admin', 'admin')
                """);
        jdbc.update("""
                INSERT INTO ppmig_contract_payment_plan_field (id, resource_id, field_id, field_value)
                VALUES ('ppmig-existing', 'ppmig-plan-c', 'ppmig-b-date', '1700000000789')
                """);

    }

    private void executeMigration(String path) throws IOException {
        var resource = new ClassPathResource(MIGRATION_PATH + path);
        String tables = "\\b(" + String.join("|", TABLES) + ")\\b";
        String sql = resource.getContentAsString(StandardCharsets.UTF_8).replaceAll(tables, "ppmig_$1");
        jdbc.execute((ConnectionCallback<Void>) connection -> {
            ScriptUtils.executeSqlScript(connection, new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8)));
            return null;
        });
    }

    private Integer legacyColumnCount() {
        return jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'ppmig_contract_payment_plan'
                  AND column_name IN ('plan_amount', 'plan_end_time')
                """, Integer.class);
    }

    private List<List<String>> fieldValues() {
        return jdbc.query("""
                SELECT resource_id, field_id, field_value
                FROM ppmig_contract_payment_plan_field
                ORDER BY resource_id, field_id
                """, (rs, rowNum) -> List.of(rs.getString(1), rs.getString(2), rs.getString(3)));
    }
}
