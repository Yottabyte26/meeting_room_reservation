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
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.mail.internet.MimeMessage;
import java.text.SimpleDateFormat;
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

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;  // 发件人邮箱

    @Value("${rbs.name:会议室预定管理系统}")
    private String systemName;  // 系统名称

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
        // 校验邮箱是否存在（重置密码场景）
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
        sendEmailAsync(email, code, type, newCode.getExpireTime());

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
    public CompletableFuture<Boolean> sendEmailAsync(String to, String code, Integer type, Date expireTime) {
        log.info("开始异步发送邮件，收件人：{}，验证码：{}", to, code);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);

            // 设置邮件主题，包含系统名称
            String subject = type == 1 ? systemName + " - 注册验证码" : systemName + " - 重置密码验证码";
            helper.setSubject(subject);

            // 构建 Thymeleaf 上下文
            Context context = new Context();
            context.setVariable("code", code);

            // 格式化时间为更友好的格式
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            context.setVariable("expireTime", dateFormat.format(expireTime));

            // 添加系统名称变量
            context.setVariable("systemName", systemName);

            // 添加操作类型描述
            context.setVariable("actionType", type == 1 ? "注册" : "重置密码");

            // 渲染 HTML 模板
            String htmlContent = templateEngine.process("email/code_email_template", context);
            helper.setText(htmlContent, true);

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

    @Override
    @Transactional
    public AjaxResult sendRegisterCode(String email) {
        // 直接调用现有方法，指定类型为1（注册）
        return sendCode(email, 1);
    }

    @Override
    @Async("taskExecutor")
    public CompletableFuture<Boolean> sendPasswordResetNotification(String email, String userName, String newPassword) {
        log.info("开始发送密码重置通知邮件，收件人：{}，用户名：{}", email, userName);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject(systemName + " - 密码重置通知");

            // 构建 Thymeleaf 上下文
            Context context = new Context();
            context.setVariable("userName", userName);
            context.setVariable("newPassword", newPassword);
            context.setVariable("systemName", systemName);

            // 渲染密码重置模板
            String htmlContent = templateEngine.process("email/password-reset", context);
            helper.setText(htmlContent, true);

            long startTime = System.currentTimeMillis();
            mailSender.send(message);
            long costTime = System.currentTimeMillis() - startTime;

            log.info("密码重置通知邮件发送成功，收件人：{}，耗时：{}ms", email, costTime);
            return CompletableFuture.completedFuture(true);
        } catch (Exception e) {
            log.error("密码重置通知邮件发送失败，收件人：{}", email, e);
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * 发送密码重置通知邮件（同步版本，供Controller调用）
     */
    @Override
    @Async("taskExecutor")
    public void sendPasswordResetNotificationSync(String email, String userName, String newPassword) {
        try {
            CompletableFuture<Boolean> future = sendPasswordResetNotification(email, userName, newPassword);
            Boolean result = future.get(); // 等待异步操作完成
            if (!result) {
                throw new RuntimeException("邮件发送失败");
            }
        } catch (Exception e) {
            log.error("发送密码重置通知邮件失败", e);
            throw new RuntimeException("邮件发送失败", e);
        }
    }
}