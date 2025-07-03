package com.rbs.web.controller.common;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import javax.annotation.Resource;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.FastByteArrayOutputStream;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.google.code.kaptcha.Producer;
import com.rbs.common.config.RuoYiConfig;
import com.rbs.common.constant.CacheConstants;
import com.rbs.common.constant.Constants;
import com.rbs.common.core.domain.AjaxResult;
import com.rbs.common.core.redis.RedisCache;
import com.rbs.common.utils.sign.Base64;
import com.rbs.common.utils.uuid.IdUtils;
import com.rbs.system.service.ISysConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 验证码操作处理
 * 
 * @author ruoyi
 */
@RestController
public class CaptchaController
{
    private static final Logger log = LoggerFactory.getLogger(CaptchaController.class);
    @Resource(name = "captchaProducer")
    private Producer captchaProducer;

    @Resource(name = "captchaProducerMath")
    private Producer captchaProducerMath;

    @Autowired
    private RedisCache redisCache;
    
    @Autowired
    private ISysConfigService configService;
    @Autowired
    private RuoYiConfig ruoYiConfig;
    /**
     * 生成验证码
     */
    @GetMapping("/captchaImage")
    public AjaxResult getCode(HttpServletResponse response) throws IOException
    {
        log.info("开始生成验证码");
        AjaxResult ajax = AjaxResult.success();
        boolean captchaEnabled = configService.selectCaptchaEnabled();
        log.info("验证码开关状态: {}", captchaEnabled);
        ajax.put("captchaEnabled", captchaEnabled);
        if (!captchaEnabled)
        {
            log.info("验证码功能关闭，直接返回结果");
            return ajax;
        }

        // 保存验证码信息
        String uuid = IdUtils.simpleUUID();
        String verifyKey = CacheConstants.CAPTCHA_CODE_KEY + uuid;

        String capStr = null, code = null;
        BufferedImage image = null;

        // 生成验证码
//        String captchaType = ruoYiConfig.getCaptchaType();
        String captchaType = "math";
        log.info("验证码类型: {}", captchaType);
        if ("math".equals(captchaType))
        {
            log.info("开始生成数学验证码");
            String capText = captchaProducerMath.createText();
            capStr = capText.substring(0, capText.lastIndexOf("@"));
            code = capText.substring(capText.lastIndexOf("@") + 1);
            image = captchaProducerMath.createImage(capStr);
        }
        else if ("char".equals(captchaType))
        {
            log.info("开始生成字符验证码");
            capStr = code = captchaProducer.createText();
            image = captchaProducer.createImage(capStr);
        }

        redisCache.setCacheObject(verifyKey, code, Constants.CAPTCHA_EXPIRATION, TimeUnit.MINUTES);
        // 转换流信息写出
        FastByteArrayOutputStream os = new FastByteArrayOutputStream();
        try
        {
            log.info("开始写入图像流");
            ImageIO.write(image, "jpg", os);
        }
        catch (IOException e)
        {
            log.error("图像写入失败", e);
            return AjaxResult.error(e.getMessage());
        }

        ajax.put("uuid", uuid);
        ajax.put("img", Base64.encode(os.toByteArray()));
        log.info("验证码生成成功");
        return ajax;
    }
}
