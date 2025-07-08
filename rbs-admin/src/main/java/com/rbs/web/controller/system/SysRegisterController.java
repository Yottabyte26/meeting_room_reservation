package com.rbs.web.controller.system;

import com.rbs.common.core.domain.entity.SysUser;
import com.rbs.system.service.ISysEmailCodeService;
import com.rbs.system.service.ISysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.rbs.common.core.controller.BaseController;
import com.rbs.common.core.domain.AjaxResult;
import com.rbs.common.core.domain.model.RegisterBody;
import com.rbs.common.utils.StringUtils;
import com.rbs.framework.web.service.SysRegisterService;
import com.rbs.system.service.ISysConfigService;

import java.util.Map;

import static com.rbs.framework.datasource.DynamicDataSourceContextHolder.log;

/**
 * 注册验证
 * 
 * @author ruoyi
 */
@RestController
public class SysRegisterController extends BaseController {
    @Autowired
    private SysRegisterService registerService;
    @Autowired
    private ISysConfigService configService;
    @Autowired
    private ISysEmailCodeService emailCodeService;
    @Autowired
    private ISysUserService userService;

    @PostMapping("/register")
    public AjaxResult register(@RequestBody RegisterBody user) {
        if (!("true".equals(configService.selectConfigByKey("sys.account.registerUser")))) {
            return error("当前系统没有开启注册功能！");
        }

        // 1. 验证邮箱验证码
        boolean isCodeValid = emailCodeService.validateCode(
                user.getEmail(),
                user.getCode(),
                1  // 注册类型
        );
        if (!isCodeValid) {
            return error("验证码错误或已过期");
        }
        // 2. 执行注册
        try {
            String msg = registerService.register(user);
            if (StringUtils.isEmpty(msg)) {
                return success("注册成功");
            } else {
                return error(msg);
            }
        } catch (Exception e) {
            log.error("注册失败", e);
            return error("注册失败：" + e.getMessage());
        }
    }

    @PostMapping("/sendCode")
    public AjaxResult sendRegisterCode(@RequestBody Map<String, String> params) {
        String email = params.get("email");
        if (StringUtils.isEmpty(email)) {
            return error("邮箱不能为空");
        }
        // 检查邮箱格式
        if (!email.matches("^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$")) {
            return error("邮箱格式不正确");
        }
        // 检查邮箱是否已被注册
        SysUser user = new SysUser();
        user.setEmail(email);
        if (!userService.checkEmailUnique(user)) {
            return error("该邮箱已被注册");
        }

        return emailCodeService.sendRegisterCode(email);
    }
}
