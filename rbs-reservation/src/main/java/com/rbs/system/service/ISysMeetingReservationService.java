package com.rbs.system.service;

import java.util.List;
import java.util.Map;

import com.rbs.system.domain.SysMeetingReservation;

/**
 * 会议室预订Service接口
 * 
 * @author why
 * @date 2025-06-11
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
}
