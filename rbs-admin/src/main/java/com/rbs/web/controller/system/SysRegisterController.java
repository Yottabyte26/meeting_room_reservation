package com.rbs.web.controller.system;

import com.rbs.system.service.ISysEmailCodeService;
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

/**
 * 注册验证
 * 
 * @author ruoyi
 */
@RestController
public class SysRegisterController extends BaseController
{
    @Autowired
    private SysRegisterService registerService;

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private ISysEmailCodeService emailCodeService;

    @PostMapping("/register")
    public AjaxResult register(@RequestBody RegisterBody user)
    {
        if (!("true".equals(configService.selectConfigByKey("sys.account.registerUser"))))
        {
            return error("当前系统没有开启注册功能！");
        }
        // 验证邮箱验证码（注册类型为1）
        boolean isCodeValid = emailCodeService.validateCode(
                user.getEmail(),  // 从注册信息中获取邮箱
                user.getCode(),   // 从注册信息中获取验证码
                1                 // 类型：1=注册
        );
        if (!isCodeValid) {
            return error("验证码错误或已过期");
        }
        // 执行注册逻辑并直接返回结果
        try {
            registerService.register(user);
            return success("注册成功");
        } catch (Exception e) {
            // 处理注册异常（如用户名重复、邮箱已注册等）
            return error("注册失败：" + e.getMessage());
        }
    }
}
