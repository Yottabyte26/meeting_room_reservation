package com.rbs.web.controller.system;

import com.rbs.common.core.domain.AjaxResult;
import com.rbs.system.domain.vo.DeptEmployeeVo;
import com.rbs.system.domain.vo.GenderVo;
import com.rbs.system.domain.vo.RoomReservationVo;
import com.rbs.system.service.ISysMeetingRoomService;
import com.rbs.system.service.ISysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.rbs.common.config.RuoYiConfig;
import com.rbs.common.utils.StringUtils;


import java.util.List;

/**
 * 首页
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/system/index")
public class SysIndexController
{
    /** 系统基础配置 */
    @Autowired
    private RuoYiConfig ruoyiConfig;

    @Autowired
    private ISysUserService userService;

    @Autowired
    private ISysMeetingRoomService meetingRoomService;
    /**
     * 访问首页，提示语
     */
    @RequestMapping("/")
    public String index()
    {
        return StringUtils.format("欢迎使用{}后台管理框架，当前版本：v{}，请通过前端地址访问。", ruoyiConfig.getName(), ruoyiConfig.getVersion());
    }

    /**
     * 获取首页部门员工统计数据
     */
    @GetMapping("/deptEmployeeStat")
    public AjaxResult getDeptEmployeeStat() {
        // 获取正常状态的员工统计数据
        List<DeptEmployeeVo> deptEmployeeList = userService.getDeptEmployeeList("0");
        return AjaxResult.success(deptEmployeeList);
    }

    /**
     * 获取用户性别统计数据
     */
    @GetMapping("/genderStat")
    public AjaxResult getGenderStat() {
        List<GenderVo> genderList = userService.getGenderList();
        return AjaxResult.success(genderList);
    }

    /**
     * 获取会议室预订次数统计数据
     */
    @GetMapping("/roomReservationStat")
    public AjaxResult getRoomReservationStat() {
        List<RoomReservationVo> reservationList = meetingRoomService.getRoomReservationList();
        return AjaxResult.success(reservationList);
    }
}
