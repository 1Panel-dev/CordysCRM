package cn.cordys.crm.system.controller;

import cn.cordys.common.pager.Pager;
import cn.cordys.common.response.handler.ResultHolder;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.LogUtils;
import cn.cordys.crm.base.BaseTest;
import cn.cordys.crm.system.domain.Announcement;
import cn.cordys.crm.system.dto.request.AnnouncementPageRequest;
import cn.cordys.crm.system.dto.request.AnnouncementRequest;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AnnouncementControllerTests extends BaseTest {
    @Resource
    private BaseMapper<Announcement> announcementMapper;

    @Test
    @Order(1)
    public void testCreateAnnouncement() throws Exception {
        AnnouncementRequest request = new AnnouncementRequest();
        request.setSubject("测试公告");
        request.setContent("测试公告内容");
        request.setStartTime(System.currentTimeMillis());
        request.setEndTime(System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 7);
        request.setDeptIds(List.of("admin"));
        request.setOrganizationId("1");
        this.requestPost("/announcement/add", request).andExpect(status().isOk());
    }

    @Test
    @Order(2)
    public void testUpdateAnnouncement() throws Exception {
        Announcement announcement = getAnnouncement();
        AnnouncementRequest request = new AnnouncementRequest();
        request.setId(announcement.getId());
        request.setSubject("更新的公告");
        request.setContent("更新的公告内容");
        request.setStartTime(System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 6);
        request.setEndTime(System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 7);
        this.requestPost("/announcement/edit", request).andExpect(status().isOk());

    }

    @Test
    @Order(3)
    public void testGetAnnouncementList() throws Exception {
        AnnouncementPageRequest basePageRequest = new AnnouncementPageRequest();
        basePageRequest.setPageSize(10);
        basePageRequest.setCurrent(1);
        basePageRequest.setOrganizationId("1");
        MvcResult mvcResult = this.requestPostWithOkAndReturn("/announcement/page", basePageRequest);
        var tableData = JSON.parseObject(JSON.toJSONString(
                        JSON.parseObject(mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8), ResultHolder.class).getData()),
                Pager.class);
        LogUtils.info(tableData.getList().toString());
       
    }

    @Test
    @Order(4)
    public void testGetAnnouncementDetail() throws Exception {
        Announcement announcement = getAnnouncement();
        MvcResult mvcResult = this.requestGetWithOkAndReturn("/announcement/get/" + announcement.getId());
        // 获取返回值
        String returnData = mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        ResultHolder resultHolder = JSON.parseObject(returnData, ResultHolder.class);
        // 返回请求正常
        Assertions.assertNotNull(resultHolder);
    }

    private Announcement getAnnouncement() {
        List<Announcement> createTime = announcementMapper.selectAll("create_time");
        return createTime.getFirst();
    }

    @Test
    @Order(5)
    public void testDeleteAnnouncement() throws Exception {
        Announcement announcement = getAnnouncement();
        requestGetWithOk("/announcement/delete/" + announcement.getId());
    }
} 