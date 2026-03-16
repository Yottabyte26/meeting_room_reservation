package com.rbs.system.mapper;

import java.time.LocalDateTime;
import java.util.List;

import com.rbs.system.domain.SysMeetingReservation;
import com.rbs.system.domain.SysMeetingRoom;
import com.rbs.system.domain.vo.RoomReservationVo;
import org.apache.ibatis.annotations.Param;

/**
 * 会议室信息Mapper接口
 * 
 * @author ruoyi
 * @date 2025-06-08
 */
public interface SysMeetingRoomMapper 
{
    /**
     * 查询会议室信息
     * 
     * @param roomId 会议室信息主键
     * @return 会议室信息
     */
    public SysMeetingRoom selectSysMeetingRoomByRoomId(Long roomId);
    public SysMeetingReservation selectSysMeetingReservationListByApplicantName(String applicantName);
    /**
     * 查询会议室信息列表
     * 
     * @param sysMeetingRoom 会议室信息
     * @return 会议室信息集合
     */
    public List<SysMeetingRoom> selectSysMeetingRoomList(SysMeetingRoom sysMeetingRoom);

    /**
     * 新增会议室信息
     * 
     * @param sysMeetingRoom 会议室信息
     * @return 结果
     */
    public int insertSysMeetingRoom(SysMeetingRoom sysMeetingRoom);

    /**
     * 修改会议室信息
     * 
     * @param sysMeetingRoom 会议室信息
     * @return 结果
     */
    public int updateSysMeetingRoom(SysMeetingRoom sysMeetingRoom);

    /**
     * 删除会议室信息
     * 
     * @param roomId 会议室信息主键
     * @return 结果
     */
    public int deleteSysMeetingRoomByRoomId(Long roomId);

    /**
     * 批量删除会议室信息
     * 
     * @param roomIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteSysMeetingRoomByRoomIds(Long[] roomIds);

    /**
     * 会议室预定次数统计
     */
    public List<RoomReservationVo> selectRoomReservationList();

    /**
     * 查询符合人数要求的空闲会议室
     * @param peopleCount 预定人数
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 空闲会议室列表
     */
    List<SysMeetingRoom> selectAvailableMeetingRooms(
            @Param("peopleCount") Integer peopleCount,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

}
