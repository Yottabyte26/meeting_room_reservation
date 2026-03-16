package com.rbs.system.controller;

import com.baidu.aip.speech.AipSpeech;
import com.rbs.common.core.controller.BaseController;
import com.rbs.common.core.domain.AjaxResult;
import com.rbs.common.utils.SecurityUtils;
import com.rbs.common.utils.VoiceCommandParser;
import com.rbs.system.domain.SysMeetingRoom;
import com.rbs.system.service.ISysMeetingReservationService;
import com.rbs.system.service.ISysMeetingRoomService;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

/**
 * 语音智能预定会议室控制器
 */
@RestController
@RequestMapping("/system/voice")
public class VoiceReservationController extends BaseController {

    @Autowired
    private ISysMeetingRoomService sysMeetingRoomService;

    @Autowired
    private ISysMeetingReservationService sysMeetingReservationService;

    @Value("${baidu.voice.app-id}")
    private String appId;

    @Value("${baidu.voice.api-key}")
    private String apiKey;

    @Value("${baidu.voice.secret-key}")
    private String secretKey;

    /**
     * 语音智能预定会议室
     */
    @PostMapping("/book")
    public AjaxResult autoBookMeetingRoom(@RequestParam("file") MultipartFile file) {
        try {
            // 1. 基础文件校验
            if (file == null || file.isEmpty()) {
                return AjaxResult.error(400, "请上传语音文件");
            }

            // 校验文件格式
            String originalFilename = file.getOriginalFilename();
            String format = "wav";
            if (originalFilename != null && originalFilename.contains(".")) {
                format = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
            }
            List<String> allowedFormats = Arrays.asList("pcm", "wav", "amr", "m4a");
            if (!allowedFormats.contains(format)) {
                return AjaxResult.error(400, "仅支持pcm/wav/amr/m4a格式的语音文件（推荐wav）");
            }

            // 2. 初始化百度ASR客户端
            AipSpeech client = new AipSpeech(appId, apiKey, secretKey);
            client.setConnectionTimeoutInMillis(5000);
            client.setSocketTimeoutInMillis(60000);

            // 3. 调用百度ASR
            HashMap<String, Object> asrParams = new HashMap<>();
            asrParams.put("dev_pid", 1536); // 普通话（支持数字）
            asrParams.put("profanity_filter", 0);
            asrParams.put("max_alternatives", 2);
            asrParams.put("rate", 16000); // 明确指定采样率
            asrParams.put("channel", 1); // 单声道
// 关键：强制指定音频格式为PCM（WAV）
            JSONObject res = client.asr(file.getBytes(), format, 16000, asrParams);
            logger.info("【百度语音识别原始返回】: {}", res.toString(2));

            // 4. 校验识别结果
            if (res.getInt("err_no") != 0) {
                logger.error("语音识别失败: {}", res.get("err_msg"));
                return AjaxResult.error(400, "语音识别失败: " + res.get("err_msg"));
            }

            // 5. 提取识别文本
            String voiceCommand = "";
            if (res.has("result") && res.getJSONArray("result").length() > 0) {
                voiceCommand = res.getJSONArray("result").getString(0);
            }

            // ========== 测试兜底（可注释） ==========
            // voiceCommand = "预定明天下午四点十人会议室";
            // ==============================================

            // 6. 文本有效性校验
            if (voiceCommand == null || voiceCommand.trim().isEmpty()) {
                return AjaxResult.error(400, "未识别到语音内容，请重新录音");
            }
            String cleanCommand = voiceCommand.replaceAll("[，。！？、！?\\s]", "");
            if (cleanCommand.length() < 2 || cleanCommand.matches("^[哦嗯啊]+$")) {
                return AjaxResult.error(400, "识别内容不清晰，请清晰说出：'预定X人的会议室 明天X点'");
            }
            logger.info("【识别到的文本】: {}", voiceCommand);

            // 7. 解析语音指令
            VoiceCommandParser.VoiceCommand command = VoiceCommandParser.parse(voiceCommand);
            logger.info("【解析结果】人数: {}, 时间: {}", command.getPeopleCount(), command.getReservationTime());

            // 8. 人数兜底
            Integer peopleCount = command.getPeopleCount();
            if (peopleCount == null) {
                peopleCount = 5;
                logger.warn("未识别到预定人数，自动使用默认值：5人");
            }

            // 9. 时间兜底
            LocalDateTime startTime = command.getReservationTime();
            if (startTime == null) {
                startTime = LocalDateTime.now().plusMinutes(30);
                logger.info("【时间兜底】未识别到时间，默认使用：{}", startTime);
            }
            LocalDateTime endTime = startTime.plusMinutes(command.getDuration());

            // 10. 查询空闲会议室
            List<SysMeetingRoom> availableRooms = sysMeetingRoomService.selectAvailableMeetingRooms(
                    peopleCount, startTime, endTime);

            if (availableRooms == null || availableRooms.isEmpty()) {
                return AjaxResult.error(400, String.format("抱歉，%s 暂无可容纳 %d 人的空闲会议室", startTime, peopleCount));
            }

            // 11. 选择最优会议室
            SysMeetingRoom bestRoom = availableRooms.get(0);

            // 12. 自动创建预定
            Long userId = SecurityUtils.getUserId();
            AjaxResult result = sysMeetingReservationService.autoCreateReservation(
                    bestRoom.getRoomId(), userId, startTime, endTime, peopleCount);

            // 13. 返回成功结果（修复：兼容Java 8的Map写法）
            if (result.isSuccess()) {
                Map<String, Object> data = new HashMap<>();
                data.put("roomName", bestRoom.getRoomName());
                data.put("peopleCount", peopleCount);
                data.put("startTime", startTime);
                data.put("endTime", endTime);
                result.put("data", data);
                result.put("msg", String.format("预定成功！已为您预定「%s」（%d人），时间：%s - %s",
                        bestRoom.getRoomName(), peopleCount, startTime, endTime));
            }

            return result;

        } catch (Exception e) {
            logger.error("语音预定会议室失败", e);
            if (e.getMessage().contains("Parameter 'peopleCount' not found")) {
                return AjaxResult.error(500, "服务器异常：Mapper参数绑定错误，请检查@Param注解");
            } else if (e.getMessage().contains("multipart boundary")) {
                return AjaxResult.error(400, "请求格式错误，请使用form-data上传文件");
            } else {
                return AjaxResult.error(500, "语音预定异常：" + e.getMessage());
            }
        }
    }

    /**
     * 解析语音指令（仅解析，不预定）
     */
    @PostMapping("/parseCommand")
    public AjaxResult parseVoiceCommand(String voiceCommand) {
        if (voiceCommand == null || voiceCommand.trim().isEmpty()) {
            return AjaxResult.error(400, "请输入语音指令文本");
        }
        VoiceCommandParser.VoiceCommand command = VoiceCommandParser.parse(voiceCommand);
        Map<String, Object> result = new HashMap<>();
        result.put("peopleCount", command.getPeopleCount());
        result.put("reservationTime", command.getReservationTime());
        result.put("duration", command.getDuration());
        return AjaxResult.success("指令解析成功", result);
    }
}