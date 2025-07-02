package com.rbs.framework.web.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import com.rbs.common.core.domain.entity.SysUser;
import com.rbs.common.core.domain.model.LoginUser;
import com.rbs.common.enums.UserStatus;
import com.rbs.common.exception.ServiceException;
import com.rbs.common.utils.MessageUtils;
import com.rbs.common.utils.StringUtils;
import com.rbs.system.service.ISysUserService;

/**
 * 用户验证处理
 *
 * @author ruoyi
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService
{
    private static final Logger log = LoggerFactory.getLogger(UserDetailsServiceImpl.class);

    @Autowired
    private ISysUserService userService;
    
    @Autowired
    private SysPasswordService passwordService;

    @Autowired
    private SysPermissionService permissionService;

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException
    {
        SysUser user = null;

        // 1. 先尝试按用户名查询
        user = userService.selectUserByUserName(identifier);

        // 2. 如果用户名查询不到，尝试按手机号查询
        if (user == null) {
            user = userService.selectUserByPhonenumber(identifier);
            if (user != null) {
                log.info("用户通过手机号登录：{}", identifier);
            }
        }

        // 3. 如果手机号也查询不到，尝试按邮箱查询
        if (user == null) {
            user = userService.selectUserByEmail(identifier);
            if (user != null) {
                log.info("用户通过邮箱登录：{}", identifier);
            }
        }

        // 4. 检查用户是否存在
        if (StringUtils.isNull(user))
        {
            log.info("登录用户：{} 不存在.", identifier);
            throw new ServiceException(MessageUtils.message("user.not.exists"));
        }
        else if (UserStatus.DELETED.getCode().equals(user.getDelFlag()))
        {
            log.info("登录用户：{} 已被删除.", identifier);
            throw new ServiceException(MessageUtils.message("user.password.delete"));
        }
        else if (UserStatus.DISABLE.getCode().equals(user.getStatus()))
        {
            log.info("登录用户：{} 已被停用.", identifier);
            throw new ServiceException(MessageUtils.message("user.blocked"));
        }

        // 验证密码
        passwordService.validate(user);

        return createLoginUser(user);
    }

    public UserDetails createLoginUser(SysUser user)
    {
        return new LoginUser(user.getUserId(), user.getDeptId(), user, permissionService.getMenuPermission(user));
    }
}
