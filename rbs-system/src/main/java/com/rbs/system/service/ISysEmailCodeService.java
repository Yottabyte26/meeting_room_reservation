package com.rbs.system.service;

import com.rbs.common.core.domain.AjaxResult;

import java.util.concurrent.CompletableFuture;

public interface ISysEmailCodeService {
    // 发送验证码（type:1-注册，2-找回密码）
    AjaxResult sendCode(String email, Integer type);

    // 专门用于注册的验证码发送接口
    AjaxResult sendRegisterCode(String email);

    // 验证验证码
    boolean validateCode(String email, String code, Integer type);
    /**
     * 发送密码重置通知邮件
     * @param email 用户邮箱
     * @param userName 用户名
     * @param newPassword 新密码
     */
    CompletableFuture<Boolean> sendPasswordResetNotification(String email, String userName, String newPassword);
    /**
     * 发送密码重置通知邮件（同步版本）
     * @param email 用户邮箱
     * @param userName 用户名
     * @param newPassword 新密码
     */
    void sendPasswordResetNotificationSync(String email, String userName, String newPassword);
}
