package com.rbs.system.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;

import com.rbs.common.utils.SecurityUtils;
import lombok.var;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.rbs.common.annotation.Log;
import com.rbs.common.core.controller.BaseController;
import com.rbs.common.core.domain.AjaxResult;
import com.rbs.common.enums.BusinessType;
import com.rbs.system.domain.SysMeetingReservation;
import com.rbs.system.service.ISysMeetingReservationService;
import com.rbs.common.utils.poi.ExcelUtil;
import com.rbs.common.core.page.TableDataInfo;

/**
 * 会议室预订Controller
 */
@RestController
@RequestMapping("/system/reservation")
public class SysMeetingReservationController extends BaseController
{
    @Autowired
    private ISysMeetingReservationService sysMeetingReservationService;

    /**
     * 获取当前用户的预订统计数据
     * @return 统计结果（total_count、pending_count等）
     */
//    @PreAuthorize("@ss.hasPermi('system:reservation:stats')")
    @GetMapping("/userStats")
    public AjaxResult getUserReservationStats() {
        Long userId = SecurityUtils.getUserId();
        Map<String, Integer> stats = sysMeetingReservationService.getUserReservationStats(userId);
        // 打印用户 ID，确认参数正确
        System.out.println("当前用户 ID: " + userId);
        // 打印查询结果
        System.out.println("统计结果: " + stats);
        // 使用HashMap手动初始化，兼容Java 1.2+
        if (stats == null) {
            stats = new HashMap<>();
            stats.put("total_count", 0);
            stats.put("pending_count", 0);
            stats.put("approved_count", 0);
            stats.put("rejected_count", 0);
        }

        return AjaxResult.success(stats);
    }


    @PreAuthorize("@ss.hasPermi('system:reservation:query')")
    @GetMapping(value="/applicant/{applicantName}")
    public TableDataInfo getlistByapplicantName(
            @PathVariable("applicantName") String applicantName,
            @RequestParam(name = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize
    ) {

        PageHelper.startPage(pageNum, pageSize);
        List<SysMeetingReservation> list = sysMeetingReservationService.selectSysMeetingReservationListByApplicantName(applicantName);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('system:reservation:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysMeetingReservation sysMeetingReservation)
    {
        startPage();
        List<SysMeetingReservation> list = sysMeetingReservationService.selectSysMeetingReservationList(sysMeetingReservation);
        return getDataTable(list);
    }
    /**
     * 导出会议室预订列表
     */
    @PreAuthorize("@ss.hasPermi('system:reservation:export')")
    @Log(title = "会议室预订", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysMeetingReservation sysMeetingReservation)
    {
        List<SysMeetingReservation> list = sysMeetingReservationService.selectSysMeetingReservationList(sysMeetingReservation);
        ExcelUtil<SysMeetingReservation> util = new ExcelUtil<SysMeetingReservation>(SysMeetingReservation.class);
        util.exportExcel(response, list, "会议室预订数据");
    }

    /**
     * 获取会议室预订详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:reservation:query')")
    @GetMapping(value = "/{reservationId}")
    public AjaxResult getInfo(@PathVariable("reservationId") Long reservationId)
    {
        return success(sysMeetingReservationService.selectSysMeetingReservationByReservationId(reservationId));
    }

    /**
     * 新增会议室预订
     */
    @PreAuthorize("@ss.hasPermi('system:reservation:add')")
    @Log(title = "会议室预订", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysMeetingReservation sysMeetingReservation)
    {
        return toAjax(sysMeetingReservationService.insertSysMeetingReservation(sysMeetingReservation));
    }

    /**
     * 修改会议室预订
     */
    @PreAuthorize("@ss.hasPermi('system:reservation:edit')")
    @Log(title = "会议室预订", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysMeetingReservation sysMeetingReservation)
    {
        return toAjax(sysMeetingReservationService.updateSysMeetingReservation(sysMeetingReservation));
    }

    /**
     * 删除会议室预订
     */
    @PreAuthorize("@ss.hasPermi('system:reservation:remove')")
    @Log(title = "会议室预订", businessType = BusinessType.DELETE)
	@DeleteMapping("/{reservationIds}")
    public AjaxResult remove(@PathVariable Long[] reservationIds)
    {
        return toAjax(sysMeetingReservationService.deleteSysMeetingReservationByReservationIds(reservationIds));
    }
}
