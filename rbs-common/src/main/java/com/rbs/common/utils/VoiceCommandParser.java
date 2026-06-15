package com.rbs.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 语音指令解析工具类
 */
public class VoiceCommandParser {

    private static final Logger log = LoggerFactory.getLogger(VoiceCommandParser.class);

    // 预定义的整点时间段（对应前端的times列表）
    private static final List<LocalTime> BOOKABLE_HOURS = new ArrayList<>();
    static {
        // 初始化可预定的整点时间（07:00, 08:00 ... 23:00）
        for (int hour = 7; hour <= 23; hour++) {
            BOOKABLE_HOURS.add(LocalTime.of(hour, 0));
        }
    }

    public static class VoiceCommand {
        private Integer peopleCount;
        private LocalDateTime reservationTime;
        private Integer duration = 60; // 默认1小时

        public Integer getPeopleCount() {
            return peopleCount;
        }

        public void setPeopleCount(Integer peopleCount) {
            this.peopleCount = peopleCount;
        }

        public LocalDateTime getReservationTime() {
            return reservationTime;
        }

        public void setReservationTime(LocalDateTime reservationTime) {
            this.reservationTime = reservationTime;
        }

        public Integer getDuration() {
            return duration;
        }

        public void setDuration(Integer duration) {
            this.duration = duration;
        }
    }

    /**
     * 解析语音指令（优化版：匹配最近的整点时间段）
     */
    public static VoiceCommand parse(String command) {
        VoiceCommand vc = new VoiceCommand();
        if (command == null || command.trim().isEmpty()) {
            return vc;
        }

        String originalCommand = command;
        log.info("【语音解析器】开始解析文本: {}", originalCommand);

        // 1. 先解析时间（核心优化：匹配最近整点时间段）
        LocalDateTime reservationTime = parseTime(command);
        vc.setReservationTime(reservationTime);

        // 2. 清理文本中的时间数字，再解析人数
        String cleanedCommand = cleanTimeNumbers(command);
        Integer peopleCount = parseChineseNumber(cleanedCommand);

        // 3. 兼容阿拉伯数字识别
        if (peopleCount == null || peopleCount <= 0 || peopleCount >= 200) {
            peopleCount = parseArabicNumber(cleanedCommand);
        }

        // 4. 校验人数范围
        if (peopleCount != null && peopleCount > 0 && peopleCount < 200) {
            vc.setPeopleCount(peopleCount);
            log.info("【语音解析器】成功提取人数: {}", peopleCount);
        } else {
            log.info("【语音解析器】未识别到有效人数");
        }

        return vc;
    }

    /**
     * 清理文本中的时间相关数字（如"九点"、"十五分"中的数字）
     */
    private static String cleanTimeNumbers(String text) {
        // 匹配 "X点"、"X时"、"X分"、"X半"、"X三刻" 并替换为空（仅替换时间部分）
        Pattern timeNumPattern = Pattern.compile("\\d+\\s*(点|时|分|半|三刻)");
        Matcher matcher = timeNumPattern.matcher(text);
        text = matcher.replaceAll("");

        // 匹配中文时间数字（如"九点钟"、"十五分"）
        Pattern chineseTimeNumPattern = Pattern.compile("[零一二三四五六七八九十]+\\s*(点|时|分|半|三刻)");
        matcher = chineseTimeNumPattern.matcher(text);
        text = matcher.replaceAll("");

        return text;
    }

    /**
     * 解析阿拉伯数字人数
     */
    private static Integer parseArabicNumber(String text) {
        Pattern pattern = Pattern.compile("(\\d{1,3})\\s*(人|个人|位|位客人|位同事)");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            try {
                String numberStr = matcher.group(1);
                int count = Integer.parseInt(numberStr);
                if (count > 0 && count < 200) {
                    return count;
                }
            } catch (NumberFormatException e) {
                log.error("【语音解析器】阿拉伯数字格式转换失败", e);
            }
        }
        return null;
    }

    /**
     * 解析中文数字（支持十五、二十五、一百等复合中文数字）
     */
    private static Integer parseChineseNumber(String text) {
        // 匹配中文数字+人数后缀（必须跟人/个/位等，避免匹配时间数字）
        Pattern chineseNumPattern = Pattern.compile("([零一二三四五六七八九十百]+)\\s*(人|个|位|位客人|位同事)");
        Matcher matcher = chineseNumPattern.matcher(text);

        if (matcher.find()) {
            String numStr = matcher.group(1).replaceAll("\\s", ""); // 去除空格
            return convertChineseNumToInt(numStr);
        }
        return null;
    }

    /**
     * 核心：将中文数字字符串转换为整数
     */
    private static Integer convertChineseNumToInt(String chineseNum) {
        // 中文数字映射
        int[] values = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 100};
        String[] units = {"一", "二", "三", "四", "五", "六", "七", "八", "九", "十", "百"};

        int result = 0;
        int temp = 0; // 临时存储百位/十位的基数

        for (int i = 0; i < chineseNum.length(); i++) {
            String c = String.valueOf(chineseNum.charAt(i));
            int idx = -1;
            for (int j = 0; j < units.length; j++) {
                if (units[j].equals(c)) {
                    idx = j;
                    break;
                }
            }
            if (idx == -1) continue; // 非数字字符跳过

            int val = values[idx];
            if (val == 10) { // 十
                temp = temp == 0 ? 10 : temp * 10;
                result += temp;
                temp = 0;
            } else if (val == 100) { // 百
                temp = temp == 0 ? 100 : temp * 100;
                result += temp;
                temp = 0;
            } else { // 一~九
                temp = val;
            }
        }
        // 处理末尾的个位（如十五的"五"、一百五十的"五"）
        result += temp;

        // 特殊处理：纯"十"=10，"零"=0
        if (chineseNum.equals("十")) result = 10;
        if (chineseNum.equals("零")) result = 0;

        return result > 0 ? result : null;
    }

    /**
     * 解析时间字符串（核心优化：匹配最近的整点时间段）
     * 核心规则：
     * 1. 用户明确指定时间（含X点/X时/X分）→ 取精确时间
     * 2. 用户未指定具体时间 → 匹配当前时间之后最近的整点时间段
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

        // 标记是否为下午/晚上（用于小时数加12）
        boolean isAfternoonOrEvening = text.contains("下午") || text.contains("晚上") || text.contains("午后");
        // 标记是否为上午（避免上午时间错误加12）
        boolean isMorning = text.contains("上午") || text.contains("早上") || text.contains("清晨");

        // 步骤 2: 精准解析具体时间
        // 2.1 匹配 "X点X分"（阿拉伯数字，如9点15分）
        Pattern timePattern1 = Pattern.compile("(\\d{1,2})(点|时)\\s*(\\d{1,2})分");
        Matcher timeMatcher1 = timePattern1.matcher(text);
        if (timeMatcher1.find()) {
            int hour = Integer.parseInt(timeMatcher1.group(1));
            int minute = Integer.parseInt(timeMatcher1.group(3));

            hour = adjustHourForPeriod(hour, isAfternoonOrEvening, isMorning);
            hour = hour > 23 ? hour % 24 : hour;
            minute = minute > 59 ? 59 : minute;

            // 整点预定：分钟数归0（匹配预定规则）
            targetTime = LocalTime.of(hour, 0);
            log.info("【语音解析器】识别到精确时间，转换为整点: {}点", hour);
        }
        // 2.2 匹配 "X点/X时"（纯小时，如9点、10时）
        else if (text.matches(".*\\d{1,2}\\s*(点|时).*")) {
            Pattern timePattern2 = Pattern.compile("(\\d{1,2})\\s*(点|时)");
            Matcher timeMatcher2 = timePattern2.matcher(text);
            if (timeMatcher2.find()) {
                int hour = Integer.parseInt(timeMatcher2.group(1));

                hour = adjustHourForPeriod(hour, isAfternoonOrEvening, isMorning);
                hour = hour > 23 ? hour % 24 : hour;

                targetTime = LocalTime.of(hour, 0);
                log.info("【语音解析器】识别到精确时间: {}点", hour);
            }
        }
        // 2.3 匹配中文时间（如九点、九点半）
        else if (text.matches(".*[零一二三四五六七八九十]+\\s*(点|时).*")) {
            Pattern chineseTimePattern = Pattern.compile("([零一二三四五六七八九十]+)\\s*(点|时)(半|三刻)?");
            Matcher chineseTimeMatcher = chineseTimePattern.matcher(text);
            if (chineseTimeMatcher.find()) {
                String chineseHour = chineseTimeMatcher.group(1);
                Integer hour = convertChineseNumToInt(chineseHour);
                if (hour == null) hour = 0;

                hour = adjustHourForPeriod(hour, isAfternoonOrEvening, isMorning);
                hour = hour > 23 ? hour % 24 : hour;

                // 整点预定：无论是否有半/三刻，都归0分钟
                targetTime = LocalTime.of(hour, 0);
                log.info("【语音解析器】识别到中文时间，转换为整点: {}点", hour);
            }
        }

        // 步骤 3: 未识别到精确时间 → 找当前时间之后最近的整点时间段
        if (targetTime == null) {
            LocalTime nowTime = LocalTime.now();
            targetTime = findNearestNextHour(nowTime);

            // 如果当天没有可用的整点了（比如23点之后），切换到次日7点
            if (targetTime == null) {
                targetTime = LocalTime.of(7, 0);
                targetDate = targetDate.plusDays(1);
                log.info("【语音解析器】当天无可用整点，默认次日7点");
            } else {
                log.info("【语音解析器】未识别到精确时间，匹配最近整点: {}", targetTime);
            }
        }

        LocalDateTime result = LocalDateTime.of(targetDate, targetTime);
        // 容错：如果解析的时间在过去（且未指定明天/后天/下周），调整为最近的可用时间
        if (result.isBefore(now) && !text.contains("明天") && !text.contains("后天") && !text.contains("大后天") && !text.contains("下周")) {
            // 重新找最近的时间
            LocalTime newTime = findNearestNextHour(LocalTime.now());
            LocalDate newDate = LocalDate.now();

            if (newTime == null) {
                newTime = LocalTime.of(7, 0);
                newDate = newDate.plusDays(1);
            }

            result = LocalDateTime.of(newDate, newTime);
            log.info("【语音解析器】解析时间在过去，自动调整为最近整点: {}", result);
        }

        log.info("【语音解析器】最终解析时间: {}", result);
        return result;
    }

    /**
     * 找到当前时间之后最近的整点时间段（仅匹配7-23点的整点）
     * @param currentTime 当前时间
     * @return 最近的整点时间，无则返回null
     */
    private static LocalTime findNearestNextHour(LocalTime currentTime) {
        LocalTime nearestTime = null;
        long minDiff = Long.MAX_VALUE;

        for (LocalTime bookableHour : BOOKABLE_HOURS) {
            // 只找当前时间之后的整点
            if (bookableHour.isAfter(currentTime)) {
                long diff = java.time.Duration.between(currentTime, bookableHour).toMinutes();
                if (diff < minDiff) {
                    minDiff = diff;
                    nearestTime = bookableHour;
                }
            }
        }

        return nearestTime;
    }

    /**
     * 根据上午/下午时段调整小时数
     */
    private static int adjustHourForPeriod(int hour, boolean isAfternoonOrEvening, boolean isMorning) {
        // 上午时间：不调整（如上午9点=9）
        if (isMorning) {
            return hour;
        }
        // 下午/晚上时间：小时数加12（12点除外，12点下午还是12点）
        if (isAfternoonOrEvening && hour != 12) {
            return hour + 12;
        }
        // 其他情况（无时段标识）：不调整
        return hour;
    }
}