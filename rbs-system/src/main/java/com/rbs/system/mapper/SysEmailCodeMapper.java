package com.rbs.system.mapper;

import com.rbs.system.domain.SysEmailCode;

public interface SysEmailCodeMapper {
    // 新增验证码记录
    int insert(SysEmailCode sysEmailCode);

    // 根据邮箱和类型删除旧验证码
    int deleteByEmailAndType(SysEmailCode sysEmailCode);

    // 根据邮箱和类型查询最新验证码
    SysEmailCode selectLatestByEmailAndType(SysEmailCode sysEmailCode);
}
