package com.rbs.system.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.rbs.common.core.domain.AjaxResult;
import com.rbs.system.domain.SysMeetingReservation;
import com.rbs.system.domain.vo.RoomReservationVo;

/**
 * 会议室预订Service接口
 */
public interface ISysMeetingReservationService
{
    /**
     * 查询会议室预订
     * 
     * @param reservationId 会议室预订主键
     * @return 会议室预订
     */
    public SysMeetingReservation selectSysMeetingReservationByReservationId(Long reservationId);
    public List<SysMeetingReservation> selectSysMeetingReservationListByApplicantName(String applicantName);
    /**
     * 查询会议室预订列表
     * 
     * @param sysMeetingReservation 会议室预订
     * @return 会议室预订集合
     */
    public List<SysMeetingReservation> selectSysMeetingReservationList(SysMeetingReservation sysMeetingReservation);

    /**
     * 新增会议室预订
     * 
     * @param sysMeetingReservation 会议室预订
     * @return 结果
     */
    public int insertSysMeetingReservation(SysMeetingReservation sysMeetingReservation);

    /**
     * 修改会议室预订
     * 
     * @param sysMeetingReservation 会议室预订
     * @return 结果
     */
    public int updateSysMeetingReservation(SysMeetingReservation sysMeetingReservation);

    /**
     * 批量删除会议室预订
     * 
     * @param reservationIds 需要删除的会议室预订主键集合
     * @return 结果
     */
    public int deleteSysMeetingReservationByReservationIds(Long[] reservationIds);

    /**
     * 删除会议室预订信息
     * 
     * @param reservationId 会议室预订主键
     * @return 结果
     */
    public int deleteSysMeetingReservationByReservationId(Long reservationId);

    /**
     * 获取当前用户的预订统计数据
     * @param userId 当前用户 ID
     * @return 统计结果
     */
    Map<String, Integer> getUserReservationStats(Long userId);

    /**
     * 自动创建会议室预定
     * @param roomId 会议室ID
     * @param userId 用户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param peopleCount 参与人数
     * @return 预定结果
     */
    AjaxResult autoCreateReservation(Long roomId, Long userId, LocalDateTime startTime, LocalDateTime endTime, Integer peopleCount);




    /**
     * 根据用户ID删除会议室预订
     * @param userId 用户ID
     * @return 结果
     */
    int deleteByUserId(Long userId);

    /**
     * 统计所有预订记录数
     * @return 总数
     */
    Integer countAllReservations();

    /**
     * 查询所有会议室预订统计
     * @return 统计结果
     */
    List<RoomReservationVo> selectAllRoomReservationStats();

}
