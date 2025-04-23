package io.cordys.crm.customer.job;

import io.cordys.crm.base.BaseTest;
import io.cordys.crm.system.job.CluePoolRecycleJob;
import io.cordys.crm.system.job.CustomerPoolRecycleJob;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RecycleJobTests extends BaseTest {

	@Resource
	private CustomerPoolRecycleJob customerPoolRecycleJob;

	@Resource
	private CluePoolRecycleJob cluePoolRecycleJob;

	@Test
	@Order(1)
	@Sql(scripts = {"/dml/init_customer_recycle_test.sql"},
			config = @SqlConfig(encoding = "utf-8", transactionMode = SqlConfig.TransactionMode.ISOLATED),
			executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
	@Sql(scripts = {"/dml/cleanup_customer_recycle_test.sql"},
			config = @SqlConfig(encoding = "utf-8", transactionMode = SqlConfig.TransactionMode.ISOLATED),
			executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
	public void testCustomerRecycle() {
		customerPoolRecycleJob.recycle();
	}

	@Test
	@Order(2)
	@Sql(scripts = {"/dml/init_clue_recycle_test.sql"},
			config = @SqlConfig(encoding = "utf-8", transactionMode = SqlConfig.TransactionMode.ISOLATED),
			executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
	@Sql(scripts = {"/dml/cleanup_clue_recycle_test.sql"},
			config = @SqlConfig(encoding = "utf-8", transactionMode = SqlConfig.TransactionMode.ISOLATED),
			executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
	public void testClueRecycle() {
		cluePoolRecycleJob.recycle();
	}
}
