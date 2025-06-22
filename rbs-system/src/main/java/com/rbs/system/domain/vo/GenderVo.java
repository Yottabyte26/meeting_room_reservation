package com.rbs.system.domain.vo;

import lombok.Data;

/**
 * 性别统计VO
 */
@Data
public class GenderVo {
    private String genderName;    // 性别名称
    private Integer genderCount;  // 人数
}