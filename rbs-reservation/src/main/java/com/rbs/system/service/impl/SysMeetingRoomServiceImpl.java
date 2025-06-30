package com.rbs.system.service.impl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.rbs.common.utils.DateUtils;
import com.rbs.system.domain.vo.RoomReservationVo;
import com.rbs.system.mapper.SysMeetingReservationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.rbs.system.mapper.SysMeetingRoomMapper;
import com.rbs.system.domain.SysMeetingRoom;
import com.rbs.system.service.ISysMeetingRoomService;

/**
 * 会议室信息Service业务层处理
 * 
 * @author ruoyi
 * @date 2025-06-08
 */
@Service
public class SysMeetingRoomServiceImpl implements ISysMeetingRoomService 
{
    @Autowired
    private SysMeetingRoomMapper sysMeetingRoomMapper;
    @Autowired
    private SysMeetingReservationMapper sysMeetingReservationMapper;

    @Override
    public List<RoomReservationVo> getRoomReservationList() {
        return sysMeetingRoomMapper.selectRoomReservationList();
    }

    /**
     * 查询会议室信息
     * 
     * @param roomId 会议室信息主键
     * @return 会议室信息
     */
    @Override
    public SysMeetingRoom selectSysMeetingRoomByRoomId(Long roomId)
    {
        return sysMeetingRoomMapper.selectSysMeetingRoomByRoomId(roomId);
    }

    /**
     * 查询会议室信息列表
     * 
     * @param sysMeetingRoom 会议室信息
     * @return 会议室信息
     */
    @Override
    public List<SysMeetingRoom> selectSysMeetingRoomList(SysMeetingRoom sysMeetingRoom)
    {
        return sysMeetingRoomMapper.selectSysMeetingRoomList(sysMeetingRoom);
    }

    /**
     * 新增会议室信息
     * 
     * @param sysMeetingRoom 会议室信息
     * @return 结果
     */
    @Override
    public int insertSysMeetingRoom(SysMeetingRoom sysMeetingRoom)
    {
        sysMeetingRoom.setCreateTime(DateUtils.getNowDate());
        return sysMeetingRoomMapper.insertSysMeetingRoom(sysMeetingRoom);
    }

    /**
     * 修改会议室信息
     * 
     * @param sysMeetingRoom 会议室信息
     * @return 结果
     */
    @Override
    public int updateSysMeetingRoom(SysMeetingRoom sysMeetingRoom)
    {
        sysMeetingRoom.setUpdateTime(DateUtils.getNowDate());
        return sysMeetingRoomMapper.updateSysMeetingRoom(sysMeetingRoom);
    }

    /**
     * 批量删除会议室信息
     * 
     * @param roomIds 需要删除的会议室信息主键
     * @return 结果
     */
    @Override
    public int deleteSysMeetingRoomByRoomIds(Long[] roomIds)
    {
        return sysMeetingRoomMapper.deleteSysMeetingRoomByRoomIds(roomIds);
    }

    /**
     * 删除会议室信息信息
     * 
     * @param roomId 会议室信息主键
     * @return 结果
     */
    @Override
    public int deleteSysMeetingRoomByRoomId(Long roomId)
    {
        return sysMeetingRoomMapper.deleteSysMeetingRoomByRoomId(roomId);
    }


    @Override
    public Integer getTotalReservations() {
        // 统计所有状态的预订总数（移除状态过滤）
        return sysMeetingReservationMapper.countAllReservations();
    }

    @Override
    public List<RoomReservationVo> getRoomReservations() {
        // 获取所有会议室
        List<SysMeetingRoom> rooms = sysMeetingRoomMapper.selectSysMeetingRoomList(null);

        // 统计每个会议室的预订次数（移除状态过滤）
        List<RoomReservationVo> stats = sysMeetingReservationMapper.selectAllRoomReservationStats();

        // 确保每个会议室都在结果中，即使预订次数为0
        Map<Long, RoomReservationVo> statMap = stats.stream()
                .collect(Collectors.toMap(RoomReservationVo::getRoomId, vo -> vo));

        return rooms.stream().map(room -> {
            RoomReservationVo vo = statMap.getOrDefault(room.getRoomId(), new RoomReservationVo());
            vo.setRoomId(room.getRoomId());
            vo.setRoomName(room.getRoomName());
            vo.setReservationCount(vo.getReservationCount() != null ? vo.getReservationCount() : 0);
            return vo;
        }).collect(Collectors.toList());
    }
}
