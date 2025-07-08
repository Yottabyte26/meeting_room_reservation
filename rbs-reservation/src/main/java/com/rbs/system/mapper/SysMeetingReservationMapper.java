package com.rbs.system.mapper;

import java.util.List;
import java.util.Map;

import com.rbs.system.domain.SysMeetingReservation;
import com.rbs.system.domain.vo.RoomReservationVo;
import io.lettuce.core.dynamic.annotation.Param;

/**
 * 会议室预订Mapper接口
 * 
 * @author why
 * @date 2025-06-11
 */
public interface SysMeetingReservationMapper 
{

    /**
     * 查询当前用户的预订统计
     * @param userId 当前用户 ID（从 Security 上下文获取）
     * @return 统计结果：{"total_count": 总数, "pending_count": 待审核, ...}
     */
    Map<String, Integer> selectUserReservationStats(@Param("userId") Long userId);

    /**
     * 查询会议室预订
     * 
     * @param reservationId 会议室预订主键
     * @return 会议室预订
     */
    public SysMeetingReservation selectSysMeetingReservationByReservationId(Long reservationId);


//    public SysMeetingReservation SysMeetingReservationselectSysMeetingReservationListByApplicantName(String applicantName);
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
     * 删除会议室预订
     * 
     * @param reservationId 会议室预订主键
     * @return 结果
     */
    public int deleteSysMeetingReservationByReservationId(Long reservationId);
    /**
     * 根据用户ID删除会议室预订记录
     *
     * @param userId 用户ID
     * @return 结果
     */
    public int deleteByUserId(Long userId);
    /**
     * 批量删除会议室预订
     * 
     * @param reservationIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteSysMeetingReservationByReservationIds(Long[] reservationIds);

    public List<SysMeetingReservation> selectSysMeetingReservationListByApplicantName(String applicantName);

    // 统计所有预订记录
    Integer countAllReservations();

    // 统计每个会议室的所有预订次数
    List<RoomReservationVo> selectAllRoomReservationStats();

}
