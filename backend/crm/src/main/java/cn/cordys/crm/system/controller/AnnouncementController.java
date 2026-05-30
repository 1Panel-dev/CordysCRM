package cn.cordys.crm.system.controller;

import cn.cordys.common.constants.PermissionConstants;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.Pager;
import cn.cordys.crm.system.dto.request.AnnouncementPageRequest;
import cn.cordys.crm.system.dto.request.AnnouncementRequest;
import cn.cordys.crm.system.dto.response.AnnouncementDTO;
import cn.cordys.crm.system.service.AnnouncementService;
import cn.cordys.security.SessionUtils;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 公告控制器，负责处理公告编辑操作。
 * <p>
 * 该控制器包含公告列表，查看，增加，编辑和删除操作。
 * </p>
 */
@RestController
@RequestMapping
@Tag(name = "公告")
public class AnnouncementController {

    @Resource
    private AnnouncementService announcementService;


    @PostMapping("/announcement/page")
    @Operation(summary = "公告列表分页查询")
    @RequiresPermissions(PermissionConstants.SYSTEM_NOTICE_READ)
    public Pager<List<AnnouncementDTO>> getAnnouncementPage(@Validated @RequestBody AnnouncementPageRequest request) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        return PageUtils.setPageInfo(page, announcementService.page(request));
    }


    /**
     * 获取公告。
     *
     * @param announcementId 公告对象。
     */
    @GetMapping(value = "/announcement/get/{announcementId}")
    @Operation(summary = "获取公告")
    @RequiresPermissions(PermissionConstants.SYSTEM_NOTICE_READ)
    public AnnouncementDTO addAnnouncement(@PathVariable String announcementId) {
        return announcementService.detail(announcementId);
    }

    /**
     * 新增公告。
     *
     * @param announcementRequest 公告对象。
     */
    @PostMapping(value = "/announcement/add")
    @RequiresPermissions(PermissionConstants.SYSTEM_NOTICE_ADD)
    @Operation(summary = "新增公告")
    public void addAnnouncement(@Validated @RequestBody AnnouncementRequest announcementRequest) {
        announcementService.add(announcementRequest, SessionUtils.getUserId());
    }

    /**
     * 编辑公告。
     *
     * @param announcementRequest 公告对象。
     */
    @PostMapping(value = "/announcement/edit")
    @Operation(summary = "编辑公告")
    @RequiresPermissions(PermissionConstants.SYSTEM_NOTICE_UPDATE)
    public void updateAnnouncement(@Validated @RequestBody AnnouncementRequest announcementRequest) {
        announcementService.update(announcementRequest, SessionUtils.getUserId());
    }

    /**
     * 删除公告。
     *
     * @param announcementId 公告id。
     */
    @GetMapping(value = "/announcement/delete/{announcementId}")
    @Operation(summary = "删除公告")
    @RequiresPermissions(PermissionConstants.SYSTEM_NOTICE_DELETE)
    public void deleteAnnouncement(@PathVariable String announcementId) {
        announcementService.delete(announcementId);
    }

}
