package com.rbs.system.service.impl;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import com.rbs.common.core.domain.AjaxResult;
import com.rbs.common.core.domain.entity.SysDept;
import com.rbs.common.core.domain.entity.SysUser;
import com.rbs.common.utils.DateUtils;
import com.rbs.common.utils.SecurityUtils;
import com.rbs.system.domain.SysMeetingRoom;
import com.rbs.system.domain.SysMeetingReservation;
import com.rbs.system.domain.vo.RoomReservationVo;
import com.rbs.system.mapper.SysMeetingRoomMapper;
import com.rbs.system.mapper.SysMeetingReservationMapper;
import com.rbs.system.service.ISysMeetingReservationService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 会议室预订Service业务层处理
 */
@Service
public class SysMeetingReservationServiceImpl implements ISysMeetingReservationService {

    // 日志对象定义（修复log符号未解析）
    private static final Logger log = LoggerFactory.getLogger(SysMeetingReservationServiceImpl.class);

    @Autowired
    private SysMeetingReservationMapper sysMeetingReservationMapper;

    @Autowired
    private SysMeetingRoomMapper sysMeetingRoomMapper;

    /**
     * 查询会议室预订
     *
     * @param reservationId 会议室预订主键
     * @return 会议室预订
     */
    @Override
    public SysMeetingReservation selectSysMeetingReservationByReservationId(Long reservationId) {
        return sysMeetingReservationMapper.selectSysMeetingReservationByReservationId(reservationId);
    }

    @Override
    public List<SysMeetingReservation> selectSysMeetingReservationListByApplicantName(String applicantName) {
        return sysMeetingReservationMapper.selectSysMeetingReservationListByApplicantName(applicantName);
    }

    /**
     * 查询会议室预订列表
     *
     * @param sysMeetingReservation 会议室预订
     * @return 会议室预订集合
     */
    @Override
    public List<SysMeetingReservation> selectSysMeetingReservationList(SysMeetingReservation sysMeetingReservation) {
        return sysMeetingReservationMapper.selectSysMeetingReservationList(sysMeetingReservation);
    }

    /**
     * 新增会议室预订
     *
     * @param sysMeetingReservation 会议室预订
     * @return 结果
     */
    @Override
    public int insertSysMeetingReservation(SysMeetingReservation sysMeetingReservation) {
        sysMeetingReservation.setCreateTime(DateUtils.getNowDate());
        return sysMeetingReservationMapper.insertSysMeetingReservation(sysMeetingReservation);
    }

    /**
     * 修改会议室预订
     *
     * @param sysMeetingReservation 会议室预订
     * @return 结果
     */
    @Override
    public int updateSysMeetingReservation(SysMeetingReservation sysMeetingReservation) {
        sysMeetingReservation.setUpdateTime(DateUtils.getNowDate());
        return sysMeetingReservationMapper.updateSysMeetingReservation(sysMeetingReservation);
    }

    /**
     * 批量删除会议室预订
     *
     * @param reservationIds 需要删除的会议室预订主键
     * @return 结果
     */
    @Override
    public int deleteSysMeetingReservationByReservationIds(Long[] reservationIds) {
        return sysMeetingReservationMapper.deleteSysMeetingReservationByReservationIds(reservationIds);
    }

    /**
     * 删除会议室预订信息
     *
     * @param reservationId 会议室预订主键
     * @return 结果
     */
    @Override
    public int deleteSysMeetingReservationByReservationId(Long reservationId) {
        return sysMeetingReservationMapper.deleteSysMeetingReservationByReservationId(reservationId);
    }

    /**
     * 获取当前用户的预订统计数据
     * @param userId 当前用户 ID
     * @return 统计结果
     */
    @Override
    public Map<String, Integer> getUserReservationStats(Long userId) {
        return sysMeetingReservationMapper.selectUserReservationStats(userId);
    }

    /**
     * 核心修复：完全匹配接口定义的方法参数（包含Long userId）
     * 逻辑优化：优先使用当前登录用户，传入的userId仅做兼容（可忽略）
     */
    @Override
    public AjaxResult autoCreateReservation(Long roomId, Long userId, LocalDateTime startTime,
                                            LocalDateTime endTime, Integer peopleCount) {
        // 1. 前置参数校验
        if (roomId == null || startTime == null || endTime == null || peopleCount == null) {
            log.warn("预定参数为空：roomId={}, userId={}, startTime={}, endTime={}, peopleCount={}",
                    roomId, userId, startTime, endTime, peopleCount);
            return AjaxResult.error("参数错误：所有必填参数不能为空");
        }
        if (startTime.isAfter(endTime)) {
            log.warn("时间参数错误：开始时间 {} 晚于结束时间 {}", startTime, endTime);
            return AjaxResult.error("参数错误：开始时间不能晚于结束时间");
        }
        if (peopleCount <= 0) {
            log.warn("人数参数错误：peopleCount={}", peopleCount);
            return AjaxResult.error("参数错误：预定人数必须大于0");
        }

        try {
            // 2. 核心：优先使用当前登录用户（忽略传入的userId，保证业务合理性）
            SysUser currentUser = SecurityUtils.getLoginUser().getUser();
            if (currentUser == null) {
                log.warn("当前用户未登录，尝试使用传入的userId={}", userId);
                return AjaxResult.error("预定失败：请先登录");
            }

            // 3. 构建预定信息（使用当前登录用户，而非传入的userId）
            SysMeetingReservation reservation = new SysMeetingReservation();
            reservation.setRoomId(roomId);
            reservation.setUserId(currentUser.getUserId()); // 强制使用当前登录用户ID
            reservation.setReservationTitle("语音自动预定-" + peopleCount + "人");
            reservation.setPurpose("语音智能预定");
            reservation.setParticipants(String.valueOf(peopleCount));
            reservation.setStatus("0");

            // 从当前用户获取名称/部门信息（无需查数据库/SysUserMapper）
            reservation.setCreateBy(currentUser.getUserName());
            reservation.setApplicantName(currentUser.getUserName());

            // 部门信息（防空指针）
            SysDept dept = currentUser.getDept();
            if (dept != null) {
                reservation.setDeptId(dept.getDeptId());
                reservation.setDeptName(dept.getDeptName());
            }

            // 时间转换 + 创建时间
            reservation.setStartTime(Date.from(startTime.atZone(ZoneId.systemDefault()).toInstant()));
            reservation.setEndTime(Date.from(endTime.atZone(ZoneId.systemDefault()).toInstant()));
            reservation.setCreateTime(DateUtils.getNowDate());

            // 4. 保存预定信息
            int insertResult = sysMeetingReservationMapper.insertSysMeetingReservation(reservation);
            if (insertResult <= 0) {
                log.error("预定插入数据库失败：roomId={}, 当前登录userId={}", roomId, currentUser.getUserId());
                return AjaxResult.error("预定失败：数据库操作异常");
            }

            // 5. 组装返回结果
            SysMeetingRoom room = sysMeetingRoomMapper.selectSysMeetingRoomByRoomId(roomId);
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("reservationId", reservation.getReservationId());
            resultData.put("roomName", room != null ? room.getRoomName() : "未知会议室");
            resultData.put("userName", currentUser.getUserName());
            resultData.put("startTime", startTime);
            resultData.put("endTime", endTime);
            resultData.put("peopleCount", peopleCount);
            resultData.put("status", "已预定");

            log.info("✅ 当前用户预定成功：userId={}, reservationId={}, roomId={}",
                    currentUser.getUserId(), reservation.getReservationId(), roomId);
            return AjaxResult.success("预定成功", resultData);

        } catch (Exception e) {
            log.error("❌ 自动预定会议室失败", e);
            return AjaxResult.error("预定异常：" + e.getMessage());
        }
    }

    // 补充接口所有未实现的方法（避免编译错误）
    @Override
    public int deleteByUserId(Long userId) {
        try {
            return sysMeetingReservationMapper.deleteByUserId(userId);
        } catch (Exception e) {
            log.warn("删除用户 {} 预定记录失败：{}", userId, e.getMessage());
            return 0;
        }
    }

    @Override
    public Integer countAllReservations() {
        try {
            return sysMeetingReservationMapper.countAllReservations();
        } catch (Exception e) {
            log.warn("统计所有预定记录失败：{}", e.getMessage());
            return 0;
        }
    }

    @Override
    public List<RoomReservationVo> selectAllRoomReservationStats() {
        try {
            return sysMeetingReservationMapper.selectAllRoomReservationStats();
        } catch (Exception e) {
            log.warn("查询会议室预定统计失败：{}", e.getMessage());
            return new ArrayList<RoomReservationVo>();
        }
    }
}