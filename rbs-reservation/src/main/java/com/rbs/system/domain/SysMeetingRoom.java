package com.rbs.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.rbs.common.annotation.Excel;
import com.rbs.common.core.domain.BaseEntity;

/**
 * 会议室信息对象 sys_meeting_room
 * 
 * @author ruoyi
 * @date 2025-06-08
 */
public class SysMeetingRoom extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 会议室ID */
    private Long roomId;

    /** 会议室名称 */
    @Excel(name = "会议室名称")
    private String roomName;

    /** 会议室编号 */
    @Excel(name = "会议室编号")
    private String roomCode;

    /** 会议室位置 */
    @Excel(name = "会议室位置")
    private String location;

    /** 容纳人数 */
    @Excel(name = "容纳人数")
    private Long capacity;

    /** 会议室类型（如小型、中型、大型） */
    @Excel(name = "会议室类型", readConverterExp = "如=小型、中型、大型")
    private String roomType;

    /** 状态（0开放 1关闭 2维护中） */
    @Excel(name = "状态", readConverterExp = "0=开放,1=关闭,2=维护中")
    private String status;

    /** 会议室描述 */
    @Excel(name = "会议室描述")
    private String description;

    /** 是否启用（0禁用 1启用） */
    @Excel(name = "是否启用", readConverterExp = "0=禁用,1=启用")
    private String isEnabled;

    /** 会议室图片路径*/
    private String roomImage;

    public String getRoomImage() {
        return roomImage;
    }

    public void setRoomImage(String roomImage) {
        this.roomImage = roomImage;
    }

    public void setRoomId(Long roomId) 
    {
        this.roomId = roomId;
    }

    public Long getRoomId() 
    {
        return roomId;
    }

    public void setRoomName(String roomName) 
    {
        this.roomName = roomName;
    }

    public String getRoomName() 
    {
        return roomName;
    }

    public void setRoomCode(String roomCode) 
    {
        this.roomCode = roomCode;
    }

    public String getRoomCode() 
    {
        return roomCode;
    }

    public void setLocation(String location) 
    {
        this.location = location;
    }

    public String getLocation() 
    {
        return location;
    }

    public void setCapacity(Long capacity) 
    {
        this.capacity = capacity;
    }

    public Long getCapacity() 
    {
        return capacity;
    }

    public void setRoomType(String roomType) 
    {
        this.roomType = roomType;
    }

    public String getRoomType() 
    {
        return roomType;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }

    public void setDescription(String description) 
    {
        this.description = description;
    }

    public String getDescription() 
    {
        return description;
    }

    public void setIsEnabled(String isEnabled) 
    {
        this.isEnabled = isEnabled;
    }

    public String getIsEnabled() 
    {
        return isEnabled;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("roomId", getRoomId())
            .append("roomName", getRoomName())
            .append("roomCode", getRoomCode())
            .append("location", getLocation())
            .append("capacity", getCapacity())
            .append("roomType", getRoomType())
            .append("status", getStatus())
            .append("description", getDescription())
            .append("isEnabled", getIsEnabled())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
