package com.rbs.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.rbs.common.annotation.Excel;
import com.rbs.common.core.domain.BaseEntity;

/**
 * 会议室预订对象 sys_meeting_reservation
 */
public class SysMeetingReservation extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 预订ID */
    private Long reservationId;

    /** 会议室ID */
    @Excel(name = "会议室ID")
    private Long roomId;

    /** 预订人ID */
    @Excel(name = "预订人ID")
    private Long userId;

    /** 预订部门ID */
    @Excel(name = "预订部门ID")
    private Long deptId;

    /** 预订主题 */
    @Excel(name = "预订主题")
    private String reservationTitle;

    /** 开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Excel(name = "开始时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /** 结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Excel(name = "结束时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    /** 预订用途 */
    @Excel(name = "预订用途")
    private String purpose;

    /** 参与人员（存储用户ID列表） */
    @Excel(name = "参与人员", readConverterExp = "存=储用户ID列表")
    private String participants;

    /** 状态（0待审批 1已通过 2已拒绝 3已取消） */
    @Excel(name = "状态", readConverterExp = "0=待审批,1=已通过,2=已拒绝,3=已取消")
    private String status;

    /** 预定人名称 */
    @Excel(name = "预定人名称")
    private String applicantName;

    /** 预定部门名称 */
    @Excel(name = "预定部门名称")
    private String deptName;

    public void setReservationId(Long reservationId) 
    {
        this.reservationId = reservationId;
    }

    public Long getReservationId() 
    {
        return reservationId;
    }

    public void setRoomId(Long roomId) 
    {
        this.roomId = roomId;
    }

    public Long getRoomId() 
    {
        return roomId;
    }

    public void setUserId(Long userId) 
    {
        this.userId = userId;
    }

    public Long getUserId() 
    {
        return userId;
    }

    public void setDeptId(Long deptId) 
    {
        this.deptId = deptId;
    }

    public Long getDeptId() 
    {
        return deptId;
    }

    public void setReservationTitle(String reservationTitle) 
    {
        this.reservationTitle = reservationTitle;
    }

    public String getReservationTitle() 
    {
        return reservationTitle;
    }

    public void setStartTime(Date startTime) 
    {
        this.startTime = startTime;
    }

    public Date getStartTime() 
    {
        return startTime;
    }

    public void setEndTime(Date endTime) 
    {
        this.endTime = endTime;
    }

    public Date getEndTime() 
    {
        return endTime;
    }

    public void setPurpose(String purpose) 
    {
        this.purpose = purpose;
    }

    public String getPurpose() 
    {
        return purpose;
    }

    public void setParticipants(String participants) 
    {
        this.participants = participants;
    }

    public String getParticipants() 
    {
        return participants;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }

    public void setApplicantName(String applicantName) 
    {
        this.applicantName = applicantName;
    }

    public String getApplicantName() 
    {
        return applicantName;
    }

    public void setDeptName(String deptName) 
    {
        this.deptName = deptName;
    }

    public String getDeptName() 
    {
        return deptName;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("reservationId", getReservationId())
            .append("roomId", getRoomId())
            .append("userId", getUserId())
            .append("deptId", getDeptId())
            .append("reservationTitle", getReservationTitle())
            .append("startTime", getStartTime())
            .append("endTime", getEndTime())
            .append("purpose", getPurpose())
            .append("participants", getParticipants())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .append("applicantName", getApplicantName())
            .append("deptName", getDeptName())
            .toString();
    }
}
