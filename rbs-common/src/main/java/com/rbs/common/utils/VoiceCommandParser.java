package com.rbs.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 语音指令解析工具类
 */
public class VoiceCommandParser {

    private static final Logger log = LoggerFactory.getLogger(VoiceCommandParser.class);

    public static class VoiceCommand {
        private Integer peopleCount;
        private LocalDateTime reservationTime;
        private Integer duration = 60; // 默认1小时

        public Integer getPeopleCount() { return peopleCount; }
        public void setPeopleCount(Integer peopleCount) { this.peopleCount = peopleCount; }
        public LocalDateTime getReservationTime() { return reservationTime; }
        public void setReservationTime(LocalDateTime reservationTime) { this.reservationTime = reservationTime; }
        public Integer getDuration() { return duration; }
        public void setDuration(Integer duration) { this.duration = duration; }
    }

    /**
     * 解析语音指令（修复：支持中文数字识别）
     */
    public static VoiceCommand parse(String command) {
        VoiceCommand vc = new VoiceCommand();
        if (command == null) {
            return vc;
        }

        log.info("【语音解析器】开始解析文本: {}", command);

        // 1. 人数识别（增强：支持中文数字+阿拉伯数字）
        Integer peopleCount = parseChineseNumber(command);
        if (peopleCount != null && peopleCount > 0 && peopleCount < 200) {
            vc.setPeopleCount(peopleCount);
            log.info("【语音解析器】成功提取人数: {}", peopleCount);
        } else {
            // 兼容原有数字识别逻辑
            Pattern pattern = Pattern.compile("(\\d{1,3})");
            Matcher matcher = pattern.matcher(command);
            if (matcher.find()) {
                try {
                    String numberStr = matcher.group();
                    int count = Integer.parseInt(numberStr);
                    if (count > 0 && count < 200) {
                        vc.setPeopleCount(count);
                        log.info("【语音解析器】成功提取人数: {}", count);
                    }
                } catch (NumberFormatException e) {
                    log.error("【语音解析器】数字格式转换失败", e);
                }
            }
        }

        // 2. 时间识别（增强版）
        vc.setReservationTime(parseTime(command));

        return vc;
    }

    /**
     * 解析中文数字（十、百等）
     */
    private static Integer parseChineseNumber(String text) {
        // 匹配 "十人"、"十个"、"十个人" 等格式
        Pattern chineseNumPattern = Pattern.compile("(十|百|千)\\s*[个人]?");
        Matcher matcher = chineseNumPattern.matcher(text);

        if (matcher.find()) {
            String numStr = matcher.group(1);
            switch (numStr) {
                case "十": return 10;
                case "百": return 100;
                case "千": return 1000;
                default: return null;
            }
        }
        return null;
    }

    /**
     * 解析时间字符串（原有逻辑保持不变）
     */
    private static LocalDateTime parseTime(String text) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate targetDate = now.toLocalDate();
        LocalTime targetTime = null;

        // --- 步骤 1: 解析日期 (确定是哪一天) ---
        if (text.contains("明天")) {
            targetDate = now.plusDays(1).toLocalDate();
        } else if (text.contains("后天")) {
            targetDate = now.plusDays(2).toLocalDate();
        } else if (text.contains("大后天")) {
            targetDate = now.plusDays(3).toLocalDate();
        } else if (text.contains("下周")) {
            int offset = 0;
            if (text.contains("一")) offset = 1;
            else if (text.contains("二")) offset = 2;
            else if (text.contains("三")) offset = 3;
            else if (text.contains("四")) offset = 4;
            else if (text.contains("五")) offset = 5;
            else if (text.contains("六")) offset = 6;
            else if (text.contains("日") || text.contains("天")) offset = 7;

            if (offset > 0) {
                targetDate = now.with(TemporalAdjusters.next(DayOfWeek.of(offset == 7 ? 7 : offset))).toLocalDate();
            }
        } else {
            targetDate = now.toLocalDate();
        }

        // 2.1 匹配 "X点"、"X点X分"、"X点半"
        Pattern timePattern = Pattern.compile("(\\d{1,2})(点|时)((\\d{1,2})分)?(半|三刻)?");
        Matcher timeMatcher = timePattern.matcher(text);

        if (timeMatcher.find()) {
            int hour = Integer.parseInt(timeMatcher.group(1));
            if (timeMatcher.group(3) != null) {
                int minute = Integer.parseInt(timeMatcher.group(4));
                targetTime = LocalTime.of(hour, minute);
            } else if ("半".equals(timeMatcher.group(5))) {
                targetTime = LocalTime.of(hour, 30);
            } else if ("三刻".equals(timeMatcher.group(5))) {
                targetTime = LocalTime.of(hour, 45);
            } else {
                targetTime = LocalTime.of(hour, 0);
            }
            log.info("【语音解析器】识别到具体时间: {}", targetTime);
        } else {
            if (text.contains("下午") || text.contains("晚上")) {
                targetTime = LocalTime.of(14, 0);
                log.info("【语音解析器】识别到下午/晚上，默认时间: 14:00");
            } else if (text.contains("上午") || text.contains("早上")) {
                targetTime = LocalTime.of(9, 0);
                log.info("【语音解析器】识别到上午/早上，默认时间: 09:00");
            } else {
                LocalTime nowTime = LocalTime.now();
                targetTime = nowTime.plusMinutes(30);
                if (targetTime.isBefore(nowTime) || nowTime.getHour() >= 23) {
                    log.info("【语音解析器】时间过晚，自动调整到明天上午9点");
                    targetTime = LocalTime.of(9, 0);
                    targetDate = targetDate.plusDays(1);
                } else {
                    log.info("【语音解析器】未识别到具体时刻，默认推后30分钟: {}", targetTime);
                }
            }
        }

        LocalDateTime result = LocalDateTime.of(targetDate, targetTime);
        if (result.isBefore(now) && !text.contains("明天") && !text.contains("后天")) {
            result = result.plusWeeks(1);
            log.info("【语音解析器】结果在过去，自动调整为下周: {}", result);
        }

        log.info("【语音解析器】最终解析时间: {}", result);
        return result;
    }
}