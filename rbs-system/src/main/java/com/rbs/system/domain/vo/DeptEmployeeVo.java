package com.rbs.system.domain.vo;

import lombok.Data;

/**
 * 部门员工统计VO
 */
@Data
public class DeptEmployeeVo {
    private String deptName;  // 部门名称
    private Integer employeeCount;  // 员工数量
}
