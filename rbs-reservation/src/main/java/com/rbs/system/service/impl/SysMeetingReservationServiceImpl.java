package com.rbs.system.service.impl;

import java.util.List;
import java.util.Map;

import com.rbs.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.rbs.system.mapper.SysMeetingReservationMapper;
import com.rbs.system.domain.SysMeetingReservation;
import com.rbs.system.service.ISysMeetingReservationService;

/**
 * 会议室预订Service业务层处理
 *
 * @author why
 * @date 2025-06-11
 */
@Service
public class SysMeetingReservationServiceImpl implements ISysMeetingReservationService {

    @Autowired
    private SysMeetingReservationMapper sysMeetingReservationMapper;

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
}