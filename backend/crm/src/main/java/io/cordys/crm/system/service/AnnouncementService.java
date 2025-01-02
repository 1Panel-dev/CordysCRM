package io.cordys.crm.system.service;

import io.cordys.aspectj.annotation.OperationLog;
import io.cordys.aspectj.constants.LogModule;
import io.cordys.aspectj.constants.LogType;
import io.cordys.common.pager.condition.BasePageRequest;
import io.cordys.common.uid.IDGenerator;
import io.cordys.crm.system.domain.Announcement;
import io.cordys.crm.system.dto.request.AnnouncementPageRequest;
import io.cordys.crm.system.dto.request.AnnouncementRequest;
import io.cordys.crm.system.dto.response.AnnouncementDTO;
import io.cordys.crm.system.mapper.ExtAnnouncementMapper;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class AnnouncementService{

    @Resource
    private BaseMapper<Announcement> announcementMapper;
    @Resource
    private ExtAnnouncementMapper extAnnouncementMapper;

    @Transactional(rollbackFor = Exception.class)
    @OperationLog(module = LogModule.SYSTEM, type = LogType.ADD, resourceId = "{{#announcement.id}}", operator = "{{#announcement.subject}}", success = "新增公告成功", extra = "{{#announcement}}")
    public void add(AnnouncementRequest request, String userId) {
        Announcement announcement = new Announcement();
        announcement.setId(IDGenerator.nextStr());
        announcement.setSubject(request.getSubject());
        announcement.setContent(request.getContent().getBytes());
        announcement.setStartTime(request.getStartTime());
        announcement.setEndTime(request.getEndTime());
        announcement.setUrl(request.getUrl());
        announcement.setReceiver(request.getReceiver());
        announcement.setOrganizationId(request.getOrganizationId());
        announcement.setCreateTime(System.currentTimeMillis());
        announcement.setUpdateTime(System.currentTimeMillis());
        announcement.setCreateUser(userId);
        announcement.setUpdateUser(userId);
        announcementMapper.insert(announcement);
    }

    @Transactional(rollbackFor = Exception.class)
    @OperationLog(module = LogModule.SYSTEM, type = LogType.UPDATE, resourceId = "{{#announcement.id}}", operator = "{{#announcement.subject}}", success = "修改公告成功", extra = "{{#announcement}}")
    public void update(AnnouncementRequest request, String userId) {
        Announcement announcement = announcementMapper.selectByPrimaryKey(request.getId());
        if (announcement == null) {
            throw new RuntimeException("公告不存在");
        }
        announcement.setSubject(request.getSubject());
        announcement.setContent(request.getContent().getBytes());
        announcement.setStartTime(request.getStartTime());
        announcement.setEndTime(request.getEndTime());
        announcement.setUrl(request.getUrl());
        announcement.setReceiver(request.getReceiver());
        announcement.setOrganizationId(request.getOrganizationId());
        announcement.setUpdateTime(System.currentTimeMillis());
        announcement.setUpdateUser(userId);
        announcementMapper.updateById(announcement);
    }

    @Transactional(rollbackFor = Exception.class)
    @OperationLog(module = LogModule.SYSTEM, type = LogType.DELETE, resourceId = "{{#id}}", operator = "{{#userId}}", success = "删除公告成功", extra = "{{#announcement}}")
    public void delete(String id, String userId) {
        Announcement announcement = announcementMapper.selectByPrimaryKey(id);
        if (announcement == null) {
            throw new RuntimeException("公告不存在");
        }
        announcementMapper.deleteByPrimaryKey(id);
    }

    /**
     * 公告列表分页查询
     * @param request
     * @return
     */
    public List<AnnouncementDTO> page(AnnouncementPageRequest request) {
        List<AnnouncementDTO> announcementDTOS = extAnnouncementMapper.selectByBaseRequest(request);
        return announcementDTOS;
    }

    /**
     * 获取公告
     * @param id
     * @return
     */
    public AnnouncementDTO detail(String id) {
        return extAnnouncementMapper.selectById(id);
    }
}

