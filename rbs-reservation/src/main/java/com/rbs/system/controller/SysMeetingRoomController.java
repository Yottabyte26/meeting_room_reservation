package com.rbs.system.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;

import com.rbs.system.domain.vo.RoomReservationVo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.rbs.common.annotation.Log;
import com.rbs.common.core.controller.BaseController;
import com.rbs.common.core.domain.AjaxResult;
import com.rbs.common.enums.BusinessType;
import com.rbs.system.domain.SysMeetingRoom;
import com.rbs.system.service.ISysMeetingRoomService;
import com.rbs.common.utils.poi.ExcelUtil;
import com.rbs.common.core.page.TableDataInfo;

/**
 * 会议室信息Controller
 * 
 * @author ruoyi
 * @date 2025-06-08
 */
@RestController
@RequestMapping("/system/room")
public class SysMeetingRoomController extends BaseController
{

    @GetMapping("/picture/upload-token")
    public Map<String, Object> getUploadToken() {
        String token = "437b90c01c9d41739d271a55a688073b";
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        return result;
    }
    @Autowired
    private ISysMeetingRoomService sysMeetingRoomService;

    /**
     * 查询会议室信息列表
     */
    @GetMapping("/list")
    public TableDataInfo list(SysMeetingRoom sysMeetingRoom)
    {
        startPage();
        List<SysMeetingRoom> list = sysMeetingRoomService.selectSysMeetingRoomList(sysMeetingRoom);
        return getDataTable(list);
    }


    /**
     * 导出会议室信息列表
     */
//    @PreAuthorize("@ss.hasPermi('system:room:export')")
    @Log(title = "会议室信息", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysMeetingRoom sysMeetingRoom)
    {
        List<SysMeetingRoom> list = sysMeetingRoomService.selectSysMeetingRoomList(sysMeetingRoom);
        ExcelUtil<SysMeetingRoom> util = new ExcelUtil<SysMeetingRoom>(SysMeetingRoom.class);
        util.exportExcel(response, list, "会议室信息数据");
    }

    /**
     * 获取会议室信息详细信息
     */
//    @PreAuthorize("@ss.hasPermi('system:room:query')")
    @GetMapping(value = "/{roomId}")
    public AjaxResult getInfo(@PathVariable("roomId") Long roomId)
    {
        return success(sysMeetingRoomService.selectSysMeetingRoomByRoomId(roomId));
    }


    /**
     * 新增会议室信息
     */
//    @PreAuthorize("@ss.hasPermi('system:room:add')")
    @Log(title = "会议室信息", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysMeetingRoom sysMeetingRoom)
    {
        String roomName = sysMeetingRoom.getRoomName();
        if(roomName.length() <= 3) {
            throw new RuntimeException("名称必须大于三个字符");
        }
        return toAjax(sysMeetingRoomService.insertSysMeetingRoom(sysMeetingRoom));
    }

    /**
     * 修改会议室信息
     */
//    @PreAuthorize("@ss.hasPermi('system:room:edit')")
    @Log(title = "会议室信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysMeetingRoom sysMeetingRoom)
    {
        return toAjax(sysMeetingRoomService.updateSysMeetingRoom(sysMeetingRoom));
    }

    /**
     * 删除会议室信息
     */
//    @PreAuthorize("@ss.hasPermi('system:room:remove')")
    @Log(title = "会议室信息", businessType = BusinessType.DELETE)
	@DeleteMapping("/{roomIds}")
    public AjaxResult remove(@PathVariable Long[] roomIds)
    {
        return toAjax(sysMeetingRoomService.deleteSysMeetingRoomByRoomIds(roomIds));
    }

    /**
     * 获取总预订次数（所有用户）
     */
    @GetMapping("/totalReservations")
    public AjaxResult getTotalReservations() {
        Integer total = sysMeetingRoomService.getTotalReservations();
        return AjaxResult.success(total);
    }

    /**
     * 获取会议室预订次数统计（所有用户）
     */
    @GetMapping("/roomReservations")
    public AjaxResult getRoomReservations() {
        List<RoomReservationVo> list = sysMeetingRoomService.getRoomReservations();
        return AjaxResult.success(list);
    }
}
