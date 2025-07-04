package com.rbs.framework.web.service;

import com.rbs.system.service.ISysEmailCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.rbs.common.constant.CacheConstants;
import com.rbs.common.constant.Constants;
import com.rbs.common.constant.UserConstants;
import com.rbs.common.core.domain.entity.SysUser;
import com.rbs.common.core.domain.model.RegisterBody;
import com.rbs.common.core.redis.RedisCache;
import com.rbs.common.exception.user.CaptchaException;
import com.rbs.common.exception.user.CaptchaExpireException;
import com.rbs.common.utils.DateUtils;
import com.rbs.common.utils.MessageUtils;
import com.rbs.common.utils.SecurityUtils;
import com.rbs.common.utils.StringUtils;
import com.rbs.framework.manager.AsyncManager;
import com.rbs.framework.manager.factory.AsyncFactory;
import com.rbs.system.service.ISysConfigService;
import com.rbs.system.service.ISysUserService;
import org.springframework.transaction.annotation.Transactional;

/**
 * 注册校验方法
 * 
 * @author ruoyi
 */
@Component
public class SysRegisterService {
    @Autowired
    private ISysUserService userService;
    @Autowired
    private ISysEmailCodeService emailCodeService;

    /**
     * 注册（使用邮箱验证码）
     */
    @Transactional
    public String register(RegisterBody registerBody) {
        String username = registerBody.getUsername();
        String password = registerBody.getPassword();
        String email = registerBody.getEmail();

        // 1. 基础字段校验
        if (StringUtils.isEmpty(username)) {
            return "用户名不能为空";
        }
        if (StringUtils.isEmpty(password)) {
            return "用户密码不能为空";
        }
        if (StringUtils.isEmpty(email)) {
            return "邮箱不能为空";
        }
        if (username.length() < UserConstants.USERNAME_MIN_LENGTH
                || username.length() > UserConstants.USERNAME_MAX_LENGTH) {
            return "账户长度必须在2到20个字符之间";
        }
        if (password.length() < UserConstants.PASSWORD_MIN_LENGTH
                || password.length() > UserConstants.PASSWORD_MAX_LENGTH) {
            return "密码长度必须在5到20个字符之间";
        }

        // 2. 唯一性校验
        SysUser sysUser = new SysUser();
        sysUser.setUserName(username);
        sysUser.setEmail(email);
        if (!userService.checkUserNameUnique(sysUser)) {
            return "用户名已存在";
        }
        if (!userService.checkEmailUnique(sysUser)) {
            return "邮箱已被注册";
        }

        // 3. 创建用户
        sysUser.setNickName(username);
        sysUser.setPassword(SecurityUtils.encryptPassword(password));
        sysUser.setPwdUpdateDate(DateUtils.getNowDate());

        boolean regFlag = userService.registerUser(sysUser);
        if (!regFlag) {
            throw new RuntimeException("用户注册失败");
        }

        // 4. 记录日志
        AsyncManager.me().execute(
                AsyncFactory.recordLogininfor(
                        username,
                        Constants.REGISTER,
                        MessageUtils.message("user.register.success")
                )
        );

        return "";
    }
}
