package com.rbs.system.service.impl;

import com.rbs.common.core.domain.AjaxResult;
import com.rbs.common.core.domain.entity.SysUser;
import com.rbs.system.domain.SysEmailCode;
import com.rbs.system.mapper.SysEmailCodeMapper;
import com.rbs.system.mapper.SysUserMapper;
import com.rbs.system.service.ISysEmailCodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

@Service
public class SysEmailCodeServiceImpl implements ISysEmailCodeService {

    private static final Logger log = LoggerFactory.getLogger(SysEmailCodeServiceImpl.class);

    @Autowired
    private SysEmailCodeMapper emailCodeMapper;

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;  // 发件人邮箱

    // 验证码有效期：5分钟
    private static final int EXPIRE_MINUTES = 5;

    @Override
    @Transactional
    public AjaxResult sendCode(String email, Integer type) {
        log.info("开始处理发送验证码请求，邮箱：{}，类型：{}", email, type);

        // 校验邮箱是否已注册（注册场景）
        if (type == 1) {
            SysUser user = userMapper.selectUserByEmail(email);
            if (user != null) {
                log.info("邮箱已注册，拒绝发送验证码：{}", email);
                return AjaxResult.error("该邮箱已被注册");
            }
        }
        // 校验邮箱是否存在（找回密码场景）
        else if (type == 2) {
            SysUser user = userMapper.selectUserByEmail(email);
            if (user == null) {
                log.info("邮箱未注册，拒绝发送验证码：{}", email);
                return AjaxResult.error("该邮箱未注册");
            }
        }

        // 生成6位随机验证码
        String code = generateCode();
        log.debug("生成验证码：{}，邮箱：{}", code, email);

        // 删除该邮箱同类型的旧验证码
        SysEmailCode oldCode = new SysEmailCode();
        oldCode.setEmail(email);
        oldCode.setType(type);
        emailCodeMapper.deleteByEmailAndType(oldCode);

        // 保存新验证码
        SysEmailCode newCode = new SysEmailCode();
        newCode.setEmail(email);
        newCode.setCode(code);
        newCode.setType(type);
        newCode.setCreateTime(new Date());
        newCode.setExpireTime(new Date(System.currentTimeMillis() + EXPIRE_MINUTES * 60 * 1000));
        emailCodeMapper.insert(newCode);
        log.info("验证码已保存到数据库，邮箱：{}，类型：{}", email, type);

        // 异步发送邮件
        sendEmailAsync(email, code, type);

        log.info("验证码请求处理完成，已开始异步发送邮件，邮箱：{}", email);
        return AjaxResult.success("验证码已发送至邮箱，5分钟内有效");
    }

    @Override
    public boolean validateCode(String email, String code, Integer type) {
        log.info("开始验证验证码，邮箱：{}，类型：{}", email, type);

        // 查询最新验证码
        SysEmailCode sysEmailCode = new SysEmailCode();
        sysEmailCode.setEmail(email);
        sysEmailCode.setType(type);
        SysEmailCode dbCode = emailCodeMapper.selectLatestByEmailAndType(sysEmailCode);

        // 验证逻辑
        if (dbCode == null) {
            log.info("验证码不存在，验证失败，邮箱：{}", email);
            return false;
        }

        if (new Date().after(dbCode.getExpireTime())) {
            log.info("验证码已过期，验证失败，邮箱：{}", email);
            return false;
        }

        boolean isValid = dbCode.getCode().equals(code);
        log.info("验证码验证结果：{}，邮箱：{}", isValid ? "成功" : "失败", email);
        return isValid;
    }

    // 生成6位随机验证码
    private String generateCode() {
        return String.format("%06d", new Random().nextInt(999999));
    }

    // 异步发送邮件
    @Async("taskExecutor") // 指定使用异步线程池
    public CompletableFuture<Boolean> sendEmailAsync(String to, String code, Integer type) {
        log.info("开始异步发送邮件，收件人：{}，验证码：{}", to, code);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(type == 1 ? "注册验证码" : "找回密码验证码");
            message.setText("您的验证码是：" + code + "，有效期5分钟，请勿泄露给他人。");

            long startTime = System.currentTimeMillis();
            mailSender.send(message);
            long costTime = System.currentTimeMillis() - startTime;

            log.info("邮件发送成功，收件人：{}，耗时：{}ms", to, costTime);
            return CompletableFuture.completedFuture(true);
        } catch (Exception e) {
            log.error("邮件发送失败，收件人：{}", to, e);
            return CompletableFuture.completedFuture(false);
        }
    }
}