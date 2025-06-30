package com.rbs.system.domain.vo;

import lombok.Data;

/**
 * 会议室预定统计VO
 */
@Data
public class RoomReservationVo {
    private String roomName;      // 会议室名称
    private Integer reservationCount; // 预定次数
    private Long roomId; // 会议室ID
}