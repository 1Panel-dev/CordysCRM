package io.cordys.crm.system.controller;

import io.cordys.crm.base.BaseTest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;

@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PicControllerTests extends BaseTest {

	private static final String BASE_PATH = "/pic";

	private static final String UPLOAD = "/upload/temp";
	private static final String PREVIEW = "/preview";

	public static List<String> tempIds;

	@Override
	protected String getBasePath() {
		return BASE_PATH;
	}

	@Test
	@Order(1)
	void uploadTmpPic() throws Exception {
		MockMultipartFile picFile = new MockMultipartFile("pic.jpg", "pic.jpg", "image/jpeg", "hello_world".getBytes());
		MultiValueMap<String, Object> paramMap = new LinkedMultiValueMap<>();
		paramMap.add("files", List.of(picFile));
		MvcResult mvcResult = this.requestMultipart(UPLOAD, paramMap).andReturn();
		tempIds = getResultDataArray(mvcResult, String.class);
	}

	@Test
	@Order(2)
	void previewPic() throws Exception {
		this.requestGetStreamWithOk(PREVIEW + "/" + tempIds.getFirst());
	}
}
