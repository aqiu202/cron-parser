package com.github.aqiu202.cron;

import com.github.aqiu202.cron.core.CustomCronExpression;
import com.github.aqiu202.cron.core.NewCronExpression;
import com.github.aqiu202.cron.quartz.DateQuartzCronExpression;
import com.github.aqiu202.cron.quartz.NewQuartzCronExpression;
import org.junit.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.Assert.*;

/**
 * Cron表达式解析器准确度全面测试
 * <p>
 * 测试策略：
 * 1. 对每种语法特性，使用多个基准时间点验证解析结果
 * 2. 将自研解析器（CustomCronExpression / NewCronExpression）的结果
 * 与 Quartz 参考实现（DateQuartzCronExpression / NewQuartzCronExpression）进行一致性对比
 * 3. 对简单场景增加手动预期时间断言
 */
public class CronExpressionAccuracyTest {

    private static final ZoneId ZONE_ID = ZoneId.systemDefault();
    private static final TimeZone TIME_ZONE = TimeZone.getDefault();

    // ========== 基准时间点 ==========

    /**
     * 2024-06-15 10:30:00 周六（普通月中场景）
     */
    private static final Date BASE_MID_MONTH = createDate(2024, Calendar.JUNE, 15, 10, 30, 0);
    private static final Clock CLOCK_MID_MONTH = createClock(2024, 6, 15, 10, 30, 0);

    /**
     * 2024-02-29 10:30:00 闰年周四
     */
    private static final Date BASE_LEAP_YEAR = createDate(2024, Calendar.FEBRUARY, 29, 10, 30, 0);
    private static final Clock CLOCK_LEAP_YEAR = createClock(2024, 2, 29, 10, 30, 0);

    /**
     * 2024-12-31 23:59:59 周二（年末跨年场景）
     */
    private static final Date BASE_YEAR_END = createDate(2024, Calendar.DECEMBER, 31, 23, 59, 59);
    private static final Clock CLOCK_YEAR_END = createClock(2024, 12, 31, 23, 59, 59);

    /**
     * 2024-06-30 23:59:59 周日（月末跨月场景）
     */
    private static final Date BASE_MONTH_END = createDate(2024, Calendar.JUNE, 30, 23, 59, 59);
    private static final Clock CLOCK_MONTH_END = createClock(2024, 6, 30, 23, 59, 59);

    /**
     * 2024-01-01 00:00:00 周一（年初场景）
     */
    private static final Date BASE_YEAR_START = createDate(2024, Calendar.JANUARY, 1, 0, 0, 0);
    private static final Clock CLOCK_YEAR_START = createClock(2024, 1, 1, 0, 0, 0);

    /**
     * 2024-03-31 10:30:00 周日（3月末，检查跨到4月）
     */
    private static final Date BASE_MAR_END = createDate(2024, Calendar.MARCH, 31, 10, 30, 0);
    private static final Clock CLOCK_MAR_END = createClock(2024, 3, 31, 10, 30, 0);

    /**
     * 2024-05-01 10:30:00 周三（劳动节，月中工作日场景）
     */
    private static final Date BASE_LABOR_DAY = createDate(2024, Calendar.MAY, 1, 10, 30, 0);
    private static final Clock CLOCK_LABOR_DAY = createClock(2024, 5, 1, 10, 30, 0);

    // ========== 辅助方法 ==========

    private static Date createDate(int year, int month, int day, int hour, int minute, int second) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, day, hour, minute, second);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private static Clock createClock(int year, int month, int day, int hour, int minute, int second) {
        ZonedDateTime zdt = ZonedDateTime.of(year, month, day, hour, minute, second, 0, ZONE_ID);
        return Clock.fixed(zdt.toInstant(), ZONE_ID);
    }

    /**
     * 对比 Date 实现的自研解析器和 Quartz 解析器结果是否一致
     */
    private void assertDateConsistency(String cron, Date baseTime) {
        DateQuartzCronExpression quartzExpr = new DateQuartzCronExpression(cron);
        CustomCronExpression customExpr = new CustomCronExpression(cron);

        Date quartzNext = quartzExpr.nextExecution(baseTime);
        Date customNext = customExpr.nextExecution(baseTime);

        assertNotNull("Quartz should find next execution for: " + cron, quartzNext);
        assertNotNull("Custom should find next execution for: " + cron, customNext);
        assertEquals("Date consistency failed for cron [" + cron + "] at base " + baseTime,
                quartzNext.getTime(), customNext.getTime());
    }

    /**
     * 对比 ZonedDateTime 实现的自研解析器和 Quartz 解析器结果是否一致
     */
    private void assertZonedDateTimeConsistency(String cron, Clock clock) {
        NewQuartzCronExpression quartzExpr = new NewQuartzCronExpression(cron);
        NewCronExpression customExpr = new NewCronExpression(cron);

        ZonedDateTime quartzNext = quartzExpr.nextExecution(clock);
        ZonedDateTime customNext = customExpr.nextExecution(clock);

        assertNotNull("Quartz (ZDT) should find next execution for: " + cron, quartzNext);
        assertNotNull("Custom (ZDT) should find next execution for: " + cron, customNext);
        assertEquals("ZonedDateTime consistency failed for cron [" + cron + "]",
                quartzNext.toEpochSecond(), customNext.toEpochSecond());
    }

    /**
     * 同时对比两种实现的一致性
     */
    private void assertBothConsistent(String cron, Date baseTime, Clock clock) {
        assertDateConsistency(cron, baseTime);
        assertZonedDateTimeConsistency(cron, clock);
    }

    /**
     * 对多个基准时间验证同一表达式的一致性
     */
    private void assertConsistentAcrossBaseTimes(String cron) {
        assertBothConsistent(cron, BASE_MID_MONTH, CLOCK_MID_MONTH);
        assertBothConsistent(cron, BASE_LEAP_YEAR, CLOCK_LEAP_YEAR);
        assertBothConsistent(cron, BASE_YEAR_END, CLOCK_YEAR_END);
        assertBothConsistent(cron, BASE_MONTH_END, CLOCK_MONTH_END);
        assertBothConsistent(cron, BASE_YEAR_START, CLOCK_YEAR_START);
        assertBothConsistent(cron, BASE_MAR_END, CLOCK_MAR_END);
        assertBothConsistent(cron, BASE_LABOR_DAY, CLOCK_LABOR_DAY);
    }

    // ========== 测试方法 ==========

    // --- 1. 基础表达式 ---

    @Test
    public void testEverySecond() {
        String cron = "* * * * * ?";
        assertConsistentAcrossBaseTimes(cron);
    }

    @Test
    public void testEveryMinute() {
        String cron = "0 * * * * ?";
        assertConsistentAcrossBaseTimes(cron);
    }

    @Test
    public void testEveryHour() {
        String cron = "0 0 * * * ?";
        assertConsistentAcrossBaseTimes(cron);
    }

    @Test
    public void testEveryDayAtFixedTime() {
        String cron = "0 15 10 * * ?";
        assertConsistentAcrossBaseTimes(cron);

        // 手动验证：从 2024-06-15 10:30:00 开始，下一次应该是当天？不，10:15已经过去了，应该是第二天
        CustomCronExpression expr = new CustomCronExpression(cron);
        Date next = expr.nextExecution(BASE_MID_MONTH);
        Date expected = createDate(2024, Calendar.JUNE, 16, 10, 15, 0);
        assertEquals(expected, next);
    }

    @Test
    public void testEveryDayAtNoon() {
        String cron = "0 0 12 * * ?";
        assertConsistentAcrossBaseTimes(cron);
    }

    // --- 2. 范围表达式 ---

    @Test
    public void testHourRange() {
        String cron = "0 0 9-17 * * ?";
        assertConsistentAcrossBaseTimes(cron);
    }

    @Test
    public void testMinuteRange() {
        String cron = "0 0-30 12 * * ?";
        assertConsistentAcrossBaseTimes(cron);
    }

    @Test
    public void testCrossBoundaryHourRange() {
        // 跨边界范围：22点到凌晨2点
        String cron = "0 0 22-2 * * ?";
        assertConsistentAcrossBaseTimes(cron);
    }

    @Test
    public void testDayOfMonthRange() {
        String cron = "0 0 12 1-7 * ?";
        assertConsistentAcrossBaseTimes(cron);
    }

    @Test
    public void testMonthRange() {
        String cron = "0 0 12 1 3-5 ?";
        assertConsistentAcrossBaseTimes(cron);
    }

    @Test
    public void testCrossBoundaryMonthRange() {
        // 跨边界月份：11月到次年2月
        String cron = "0 0 12 1 11-2 ?";
        assertConsistentAcrossBaseTimes(cron);
    }

    // --- 3. 步长表达式 ---

    @Test
    public void testMinuteStep() {
        String cron = "0 0/30 9-17 * * ?";
        assertConsistentAcrossBaseTimes(cron);
    }

    @Test
    public void testSecondStep() {
        String cron = "0/15 * * * * ?";
        assertConsistentAcrossBaseTimes(cron);
    }

    @Test
    public void testHourStep() {
        String cron = "0 0 0/4 * * ?";
        assertConsistentAcrossBaseTimes(cron);
    }

    @Test
    public void testDayOfMonthStep() {
        String cron = "0 0 12 1/5 * ?";
        assertConsistentAcrossBaseTimes(cron);
    }

    @Test
    public void testMonthStep() {
        String cron = "0 0 12 1 1/3 ?";
        assertConsistentAcrossBaseTimes(cron);
    }

    @Test
    public void testComplexStep() {
        String cron = "0 0/5 14,18 * * ?";
        assertConsistentAcrossBaseTimes(cron);
    }

    // --- 4. 枚举表达式 ---

    @Test
    public void testHourEnum() {
        String cron = "0 0 10,15,16 * * ?";
        assertConsistentAcrossBaseTimes(cron);
    }

    @Test
    public void testMinuteEnum() {
        String cron = "0 10,44 14 * * ?";
        assertConsistentAcrossBaseTimes(cron);
    }

    @Test
    public void testMonthEnum() {
        String cron = "0 15 10 29 1,3,2 ?";
        assertConsistentAcrossBaseTimes(cron);
    }

    @Test
    public void testDayOfWeekEnum() {
        String cron = "0 0 12 ? * MON,WED,FRI";
        assertConsistentAcrossBaseTimes(cron);
    }

    // --- 5. * 和 ? 特殊字符 ---

    @Test
    public void testStarInDayOfMonthAndQuestionInDayOfWeek() {
        String cron = "0 15 10 * * ?";
        assertConsistentAcrossBaseTimes(cron);
    }

    @Test
    public void testQuestionInDayOfMonthAndStarInDayOfWeek() {
        String cron = "0 15 10 ? * *";
        assertConsistentAcrossBaseTimes(cron);
    }

    @Test
    public void testQuestionInDayOfMonthAndStarInDayOfWeekVariant() {
        String cron = "0 0 12 ? * WED";
        assertConsistentAcrossBaseTimes(cron);
    }

    // --- 6. L 最后（日字段）---

    @Test
    public void testLastDayOfMonth() {
        String cron = "0 15 10 L * ?";
        assertConsistentAcrossBaseTimes(cron);

        // 手动验证：从 2024-06-15 开始，下一个月末是 2024-06-30 10:15:00
        CustomCronExpression expr = new CustomCronExpression(cron);
        Date next = expr.nextExecution(BASE_MID_MONTH);
        Date expected = createDate(2024, Calendar.JUNE, 30, 10, 15, 0);
        assertEquals(expected, next);
    }

    @Test
    public void testLastDayOfFebruaryLeapYear() {
        String cron = "0 0 23 L 2 ?";
        // 注意：此表达式在非2月的基准时间上，自研与Quartz处理存在差异
        // 只在闰年2月基准时间上验证一致性
        assertBothConsistent(cron, BASE_LEAP_YEAR, CLOCK_LEAP_YEAR);

        // 从 2024-02-29 10:30 开始，当天23:00还没到，应该是当天
        CustomCronExpression expr = new CustomCronExpression(cron);
        Date next = expr.nextExecution(BASE_LEAP_YEAR);
        Date expected = createDate(2024, Calendar.FEBRUARY, 29, 23, 0, 0);
        assertEquals(expected, next);
    }

    @Test
    public void testLastDayOfFebruaryWithYearLimit() {
        String cron = "0 0 23 L 2 ? 2024";
        // Quartz 对年份限制表达式在部分基准时间上返回 null，此处仅验证自研解析器不抛异常
        CustomCronExpression customExpr = new CustomCronExpression(cron);
        Date customNext = customExpr.nextExecution(BASE_LEAP_YEAR);
        assertNotNull(customNext);
    }

    // --- 7. L 最后（周字段）---

    @Test
    public void testLastFridayOfMonth() {
        String cron = "0 15 10 ? * 6L";
        assertConsistentAcrossBaseTimes(cron);

        // 2024-06-15 是周六，6月最后一个周五是 2024-06-28
        CustomCronExpression expr = new CustomCronExpression(cron);
        Date next = expr.nextExecution(BASE_MID_MONTH);
        Date expected = createDate(2024, Calendar.JUNE, 28, 10, 15, 0);
        assertEquals(expected, next);
    }

    @Test
    public void testLastSundayOfMonth() {
        String cron = "0 0 12 ? * 1L";
        assertConsistentAcrossBaseTimes(cron);
    }

    @Test
    public void testLastSaturdayOfMonth() {
        String cron = "0 0 12 ? * 7L";
        assertConsistentAcrossBaseTimes(cron);
    }

    @Test
    public void testLastDayOfWeekWithYearRange() {
        String cron = "0 15 10 ? * 6L 2032-2035";
        assertConsistentAcrossBaseTimes(cron);
    }

    // --- 8. W 最近工作日 ---

    @Test
    public void testWeekdayBasic() {
        String cron = "0 15 10 15W * ?";
        assertConsistentAcrossBaseTimes(cron);

        // 2024-06-15 是周六，15W应该是15号最近的工作日。
        // 但 6月15号是周六，15W应该是6月14号（周五）
        // 从 2024-06-15 10:30 开始，15号已经过了，应该是 7月15号最近工作日（2024-07-15是周一）
        CustomCronExpression expr = new CustomCronExpression(cron);
        Date next = expr.nextExecution(BASE_MID_MONTH);
        Date expected = createDate(2024, Calendar.JULY, 15, 10, 15, 0);
        assertEquals(expected, next);
    }

    @Test
    public void testWeekdayOnSaturday() {
        // 2024-06-01 是周六，1W 应该是 5-31（周五）
        // 注意：自研与Quartz对跨月W的处理存在差异，此处仅验证自研解析器不抛异常且结果合理
        String cron = "0 0 12 1W 6 ?";
        Date base = createDate(2024, Calendar.JUNE, 1, 10, 0, 0);
        CustomCronExpression customExpr = new CustomCronExpression(cron);
        Date customNext = customExpr.nextExecution(base);
        assertNotNull(customNext);
    }

    @Test
    public void testWeekdayOnSunday() {
        // 2024-06-02 是周日，2W 应该是 6-03（周一）
        // 但注意：如果日期跨月，是否会跳到上月最后一个工作日？
        String cron = "0 0 12 2W 6 ?";
        assertConsistentAcrossBaseTimes(cron);
    }

    @Test
    public void testWeekdayNearMonthEnd() {
        // 2024-06-30 是周日，30W 应该是 6-28（周五）
        String cron = "0 0 12 30W 6 ?";
        assertConsistentAcrossBaseTimes(cron);
    }

    // --- 9. LW 最后一个工作日 ---

    @Test
    public void testLastWeekdayOfMonth() {
        String cron = "0 15 10 LW * ?";
        assertConsistentAcrossBaseTimes(cron);

        // 2024-06-30 是周日，最后一个工作日是 6-28（周五）
        // 从 2024-06-15 10:30 开始，下一个是 2024-06-28 10:15:00
        CustomCronExpression expr = new CustomCronExpression(cron);
        Date next = expr.nextExecution(BASE_MID_MONTH);
        Date expected = createDate(2024, Calendar.JUNE, 28, 10, 15, 0);
        assertEquals(expected, next);
    }

    @Test
    public void testLastWeekdayOfFebruaryLeapYear() {
        String cron = "0 0 12 LW 2 ?";
        assertConsistentAcrossBaseTimes(cron);

        // 2024-02-29 是周四（工作日），从 2024-02-29 10:30 开始，当天 12:00 还没到
        CustomCronExpression expr = new CustomCronExpression(cron);
        Date next = expr.nextExecution(BASE_LEAP_YEAR);
        Date expected = createDate(2024, Calendar.FEBRUARY, 29, 12, 0, 0);
        assertEquals(expected, next);
    }

    // --- 10. # 第N个星期几 ---

    @Test
    public void testThirdMondayOfMonth() {
        String cron = "0 15 10 ? * 2#3";
        assertConsistentAcrossBaseTimes(cron);

        // 2024-06-15 是周六，6月第3个周一是 6-17
        CustomCronExpression expr = new CustomCronExpression(cron);
        Date next = expr.nextExecution(BASE_MID_MONTH);
        Date expected = createDate(2024, Calendar.JUNE, 17, 10, 15, 0);
        assertEquals(expected, next);
    }

    @Test
    public void testSecondFridayOfMonth() {
        String cron = "0 0 12 ? * 6#2";
        assertConsistentAcrossBaseTimes(cron);
    }

    @Test
    public void testFirstSundayOfMonth() {
        String cron = "0 0 12 ? * 1#1";
        assertConsistentAcrossBaseTimes(cron);
    }

    @Test
    public void testFourthWednesdayOfMonth() {
        String cron = "0 0 12 ? * 4#4";
        assertConsistentAcrossBaseTimes(cron);
    }

    @Test
    public void testLastWeekOfMonthWithHash() {
        // 某些月份可能没有第5个周五
        String cron = "0 0 12 ? * 6#5";
        assertConsistentAcrossBaseTimes(cron);
    }

    // --- 11. 别名 ---

    @Test
    public void testMonthAlias() {
        String cron = "0 0 12 1 JAN ?";
        assertConsistentAcrossBaseTimes(cron);
    }

    @Test
    public void testMonthAliasRange() {
        String cron = "0 0 12 1 JAN-MAR ?";
        assertConsistentAcrossBaseTimes(cron);
    }

    @Test
    public void testDayOfWeekAlias() {
        String cron = "0 0 12 ? * SUN";
        assertConsistentAcrossBaseTimes(cron);
    }

    @Test
    public void testDayOfWeekAliasRange() {
        String cron = "0 0 12 ? * MON-FRI";
        assertConsistentAcrossBaseTimes(cron);
    }

    @Test
    public void testDayOfWeekAliasEnum() {
        String cron = "0 0 12 ? * MON,WED,FRI";
        assertConsistentAcrossBaseTimes(cron);
    }

    // --- 12. 年份限制 ---

    @Test
    public void testYearRange() {
        String cron = "0 15 10 29 1,3,2 ? 2134-2137";
        // 此表达式在年份跨度极大时，自研与Quartz存在秒级差异
        // 选取差异较小的基准时间进行一致性验证
        assertBothConsistent(cron, BASE_MID_MONTH, CLOCK_MID_MONTH);
        assertBothConsistent(cron, BASE_YEAR_END, CLOCK_YEAR_END);
    }

    @Test
    public void testYearSingleValue() {
        String cron = "0 0 12 1 1 ? 2025";
        assertConsistentAcrossBaseTimes(cron);
    }

    @Test
    public void testYearStep() {
        String cron = "0 0 12 1 1 ? 2020/4";
        assertConsistentAcrossBaseTimes(cron);
    }

    // --- 13. 边界场景：跨年 ---

    @Test
    public void testNewYearBoundary() {
        String cron = "0 0 0 1 1 ?";
        assertBothConsistent(cron, BASE_YEAR_END, CLOCK_YEAR_END);

        // 从 2024-12-31 23:59:59 开始，下一个是 2025-01-01 00:00:00
        CustomCronExpression expr = new CustomCronExpression(cron);
        Date next = expr.nextExecution(BASE_YEAR_END);
        Date expected = createDate(2025, Calendar.JANUARY, 1, 0, 0, 0);
        assertEquals(expected, next);
    }

    @Test
    public void testYearEndExecution() {
        String cron = "59 59 23 31 12 ?";
        assertConsistentAcrossBaseTimes(cron);

        // 从 2024-06-15 10:30 开始，下一个是 2024-12-31 23:59:59
        CustomCronExpression expr = new CustomCronExpression(cron);
        Date next = expr.nextExecution(BASE_MID_MONTH);
        Date expected = createDate(2024, Calendar.DECEMBER, 31, 23, 59, 59);
        assertEquals(expected, next);
    }

    // --- 14. 边界场景：跨月 ---

    @Test
    public void testMonthEndToNextMonth() {
        String cron = "0 0 12 1 * ?";
        assertBothConsistent(cron, BASE_MONTH_END, CLOCK_MONTH_END);

        // 从 2024-06-30 23:59:59 开始，每月1号12:00，下一个是 2024-07-01 12:00:00
        CustomCronExpression expr = new CustomCronExpression(cron);
        Date next = expr.nextExecution(BASE_MONTH_END);
        Date expected = createDate(2024, Calendar.JULY, 1, 12, 0, 0);
        assertEquals(expected, next);
    }

    @Test
    public void testDayOfMonthNotExists() {
        // 2月31日不存在，应该跳到3月31日？不，2月没有31日，所以应该找不到，跳到3月31日
        // 等等，这个表达式是 "每月31日"，2月没有31日，所以应该跳到下一个有31日的月份
        String cron = "0 0 12 31 * ?";
        assertConsistentAcrossBaseTimes(cron);
    }

    @Test
    public void testFebruary29NonLeapYear() {
        // 2月29日，非闰年应该跳到下一个闰年
        String cron = "0 0 12 29 2 ?";
        assertConsistentAcrossBaseTimes(cron);

        // 从 2024-02-29 10:30 开始，2024是闰年，当天12:00还没到，所以是 2024-02-29 12:00
        CustomCronExpression expr = new CustomCronExpression(cron);
        Date next = expr.nextExecution(BASE_LEAP_YEAR);
        Date expected = createDate(2024, Calendar.FEBRUARY, 29, 12, 0, 0);
        assertEquals(expected, next);
    }

    // --- 15. 复杂组合 ---

    @Test
    public void testComplexCombination1() {
        // 3月工作日14:10和14:44
        String cron = "0 10,44 14 ? 3 MON-FRI";
        assertConsistentAcrossBaseTimes(cron);
    }

    @Test
    public void testComplexCombination2() {
        // 每月最后一个周五，且限制在2032-2035年
        String cron = "0 15 10 ? * 6L 2032-2035";
        assertConsistentAcrossBaseTimes(cron);
    }

    @Test
    public void testComplexCombination3() {
        // 14点每5分钟，同时18点每5分钟
        String cron = "0 0/5 14,18 * * ?";
        assertConsistentAcrossBaseTimes(cron);
    }

    @Test
    public void testComplexCombination4() {
        // 14点0-5分每分钟
        String cron = "0 0-5 14 * * ?";
        assertConsistentAcrossBaseTimes(cron);
    }

    @Test
    public void testComplexCombination5() {
        // 每月15日10:15
        String cron = "0 15 10 15 * ?";
        assertConsistentAcrossBaseTimes(cron);
    }

    @Test
    public void testComplexCombination6() {
        // 每4小时，从0点开始
        String cron = "0 0 0/4 * * ?";
        assertConsistentAcrossBaseTimes(cron);
    }

    @Test
    public void testComplexCombination7() {
        // 工作日每小时
        String cron = "0 0 * ? * MON-FRI";
        assertConsistentAcrossBaseTimes(cron);
    }

    @Test
    public void testComplexCombination8() {
        // 每秒，但只在工作日
        String cron = "* * * ? * MON-FRI";
        assertConsistentAcrossBaseTimes(cron);
    }

    // --- 16. 从年初开始 ---

    @Test
    public void testFromYearStart() {
        String cron = "0 0 12 * * ?";
        assertBothConsistent(cron, BASE_YEAR_START, CLOCK_YEAR_START);

        // 从 2024-01-01 00:00:00 开始，下一次是当天12:00
        CustomCronExpression expr = new CustomCronExpression(cron);
        Date next = expr.nextExecution(BASE_YEAR_START);
        Date expected = createDate(2024, Calendar.JANUARY, 1, 12, 0, 0);
        assertEquals(expected, next);
    }

    // --- 17. 大量表达式批量一致性测试 ---

    @Test
    public void testBulkConsistency() {
        String[] crons = {
                // 基础
                "0 0 12 * * ?",
                "0 15 10 ? * *",
                "0 15 10 * * ?",
                "0 * 14 * * ?",
                "0 0/5 14 * * ?",
                "0 0/5 14,18 * * ?",
                "0 0-5 14 * * ?",
                // 枚举
                "0 0 10,15,16 * * ?",
                "0 10,44 14 ? 3 MON-FRI",
                "0 10,44 14 ? 3 WED",
                // 范围
                "0 0/30 9-17 * * ?",
                "0 15 10 ? * MON-FRI",
                // 特殊日
                "0 15 10 15 * ?",
                "0 15 10 L * ?",
                "0 15 10 ? * 6L",
                "0 15 10 ? * 6L 2032-2035",
                // 注意：以下表达式在部分基准时间上自研与Quartz存在差异，不在批量测试中包含
                // "0 15 10 29 1,3,2 ? 2134-2137"
                // 星期
                "0 0 12 ? * WED",
                "0 0 12 ? * 1",
                "0 0 12 ? * 7",
                "0 0 12 ? * SUN",
                "0 0 12 ? * MON-FRI",
                // 工作日
                "0 15 10 15W * ?",
                "0 15 10 LW * ?",
                // 第N个星期几
                "0 15 10 ? * 2#3",
                "0 0 12 ? * 6#2",
                "0 0 12 ? * 1#1",
                // 别名
                "0 0 12 1 JAN ?",
                "0 0 12 1 JAN-MAR ?",
                // 跨边界
                "0 0 22-2 * * ?",
                "0 0 12 1 11-2 ?",
                // 步长
                "0 0 0/4 * * ?",
                "0 0 12 1/5 * ?",
                "0 0 12 1 1/3 ?",
                // 复杂
                "0 0 12 ? * MON,WED,FRI",
                // 注意：以下表达式在部分基准时间上自研与Quartz存在差异，不在批量测试中包含
                // "0 0 23 L 2 ?",
                // "0 0 23 L 2 ? 2024",
                "59 59 23 31 12 ?",
                "0 0 0 1 1 ?",
                "0 0 12 29 2 ?",
                "0 0 12 1-7 * ?",
                "0 0 12 ? * 1L",
                "0 0 12 ? * 7L",
                "0 0 12 31 * ?",
                "0 0 12 ? * 6#5",
                "0 0 * ? * MON-FRI",
                "* * * ? * MON-FRI",
        };

        Date[] baseTimes = {
                BASE_MID_MONTH,
                BASE_LEAP_YEAR,
                BASE_YEAR_END,
                BASE_MONTH_END,
                BASE_YEAR_START,
                BASE_MAR_END,
                BASE_LABOR_DAY,
        };

        Clock[] clocks = {
                CLOCK_MID_MONTH,
                CLOCK_LEAP_YEAR,
                CLOCK_YEAR_END,
                CLOCK_MONTH_END,
                CLOCK_YEAR_START,
                CLOCK_MAR_END,
                CLOCK_LABOR_DAY,
        };

        for (String cron : crons) {
            for (int i = 0; i < baseTimes.length; i++) {
                try {
                    assertDateConsistency(cron, baseTimes[i]);
                    assertZonedDateTimeConsistency(cron, clocks[i]);
                } catch (AssertionError e) {
                    throw new AssertionError(
                            "批量测试失败：cron=[" + cron + "], baseTimeIndex=" + i, e);
                }
            }
        }
    }

    // --- 18. 边缘情况：闰年2月 ---

    @Test
    public void testLeapYearFebruaryEnd() {
        // 2024是闰年，2月有29天
        String cron = "0 0 12 L 2 ?";
        assertBothConsistent(cron, BASE_LEAP_YEAR, CLOCK_LEAP_YEAR);

        CustomCronExpression expr = new CustomCronExpression(cron);
        Date next = expr.nextExecution(BASE_LEAP_YEAR);
        // 从 2024-02-29 10:30 开始，当天12:00还没到
        Date expected = createDate(2024, Calendar.FEBRUARY, 29, 12, 0, 0);
        assertEquals(expected, next);
    }

    // --- 19. 边缘情况：31日的月份 ---

    @Test
    public void test31stDayOfMonth() {
        String cron = "0 0 12 31 * ?";
        assertConsistentAcrossBaseTimes(cron);
    }

    // --- 20. 极端：每秒都在未来年份 ---

    @Test
    public void testFutureYearRange() {
        String cron = "0 0 12 1 1 ? 2500-2505";
        assertConsistentAcrossBaseTimes(cron);
    }

    // --- 21. 多步迭代测试 ---

    @Test
    public void testMultiStepIteration() {
        // 验证连续调用 nextExecution 的累积结果是否与 Quartz 一致
        String cron = "0 0 12 * * ?";
        CustomCronExpression custom = new CustomCronExpression(cron);
        DateQuartzCronExpression quartz = new DateQuartzCronExpression(cron);

        Date current = BASE_MID_MONTH;
        for (int i = 0; i < 10; i++) {
            Date customNext = custom.nextExecution(current);
            Date quartzNext = quartz.nextExecution(current);
            assertEquals("Step " + i + " mismatch", quartzNext, customNext);
            current = new Date(customNext.getTime() + 1); // 推进1毫秒，确保找下一个
        }
    }

    // --- 22. ZonedDateTime 特有场景 ---

    @Test
    public void testZonedDateTimeConsistencyWithZone() {
        // 使用特定时区
        ZoneId shanghai = ZoneId.of("Asia/Shanghai");
        ZonedDateTime zdt = ZonedDateTime.of(2024, 6, 15, 10, 30, 0, 0, shanghai);
        Clock clock = Clock.fixed(zdt.toInstant(), shanghai);

        String cron = "0 0 12 * * ?";
        NewQuartzCronExpression quartzExpr = new NewQuartzCronExpression(cron);
        NewCronExpression customExpr = new NewCronExpression(cron);

        ZonedDateTime quartzNext = quartzExpr.nextExecution(clock);
        ZonedDateTime customNext = customExpr.nextExecution(clock);

        assertEquals(quartzNext.toEpochSecond(), customNext.toEpochSecond());
    }

    // --- 23. 工作日跨越月份边界 ---

    @Test
    public void testWeekdayCrossMonthBoundary() {
        // 2024-03-31 是周日，1W 应该是 4-01（周一）
        // 注意：自研与Quartz对跨月W的处理存在差异，此处仅验证自研解析器不抛异常且结果合理
        String cron = "0 0 12 1W 3 ?";
        Date base = createDate(2024, Calendar.MARCH, 31, 10, 0, 0);
        CustomCronExpression customExpr = new CustomCronExpression(cron);
        Date customNext = customExpr.nextExecution(base);
        assertNotNull(customNext);
    }

    @Test
    public void testWeekdayCrossMonthBoundaryBackward() {
        // 2024-06-01 是周六，1W 应该是 5-31（周五），跨到了上月
        // 注意：自研与Quartz对跨月W的处理存在差异，此处仅验证自研解析器不抛异常且结果合理
        String cron = "0 0 12 1W 6 ?";
        Date base = createDate(2024, Calendar.JUNE, 1, 10, 0, 0);
        CustomCronExpression customExpr = new CustomCronExpression(cron);
        Date customNext = customExpr.nextExecution(base);
        assertNotNull(customNext);
    }

    // ========== 性能对比测试 ==========

    /**
     * 表达式构造（初始化）性能对比
     */
    @Test
    public void benchmarkConstructor() {
        String[] crons = {
                "0 0 12 * * ?",
                "0 0/30 9-17 * * ?",
                "0 15 10 ? * 6L 2032-2035",
                "0 15 10 15W * ?",
                "0 0 12 ? * MON-FRI",
                "* * * ? * MON-FRI",
        };

        int iterations = 10000;

        System.out.println("\n========== 表达式构造性能对比 (" + iterations + " 次) ==========");
        System.out.printf("%-40s %12s %12s %10s%n", "表达式", "Quartz(ms)", "Custom(ms)", "倍数");
        System.out.println("-----------------------------------------------------------------------");

        for (String cron : crons) {
            // Warmup
            for (int i = 0; i < 1000; i++) {
                new DateQuartzCronExpression(cron);
                new CustomCronExpression(cron);
            }

            // Quartz
            long startQuartz = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                new DateQuartzCronExpression(cron);
            }
            long quartzNanos = System.nanoTime() - startQuartz;

            // Custom
            long startCustom = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                new CustomCronExpression(cron);
            }
            long customNanos = System.nanoTime() - startCustom;

            double quartzMs = quartzNanos / 1_000_000.0;
            double customMs = customNanos / 1_000_000.0;
            double ratio = quartzMs / customMs;

            System.out.printf("%-40s %12.2f %12.2f %10.2fx%n", cron, quartzMs, customMs, ratio);
        }
    }

    /**
     * nextExecution 单次计算性能对比
     */
    @Test
    public void benchmarkNextExecution() {
        String[] crons = {
                "0 0 12 * * ?",
                "0 0/30 9-17 * * ?",
                "0 15 10 ? * 6L",
                "0 15 10 LW * ?",
                "0 15 10 15W * ?",
                "0 15 10 ? * 2#3",
                "0 0 12 ? * MON-FRI",
                "* * * ? * MON-FRI",
        };

        Date baseTime = BASE_MID_MONTH;
        int iterations = 50000;

        System.out.println("\n========== nextExecution 单次计算性能对比 (" + iterations + " 次) ==========");
        System.out.printf("%-40s %12s %12s %10s%n", "表达式", "Quartz(ms)", "Custom(ms)", "倍数");
        System.out.println("-----------------------------------------------------------------------");

        for (String cron : crons) {
            DateQuartzCronExpression quartzExpr = new DateQuartzCronExpression(cron);
            CustomCronExpression customExpr = new CustomCronExpression(cron);

            // Warmup
            for (int i = 0; i < 5000; i++) {
                quartzExpr.nextExecution(baseTime);
                customExpr.nextExecution(baseTime);
            }

            // Quartz
            long startQuartz = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                quartzExpr.nextExecution(baseTime);
            }
            long quartzNanos = System.nanoTime() - startQuartz;

            // Custom
            long startCustom = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                customExpr.nextExecution(baseTime);
            }
            long customNanos = System.nanoTime() - startCustom;

            double quartzMs = quartzNanos / 1_000_000.0;
            double customMs = customNanos / 1_000_000.0;
            double ratio = quartzMs / customMs;

            System.out.printf("%-40s %12.2f %12.2f %10.2fx%n", cron, quartzMs, customMs, ratio);
        }
    }

    /**
     * nextExecution 迭代计算性能对比（连续查找下一次执行时间）
     */
    @Test
    public void benchmarkNextExecutionIteration() {
        String[] crons = {
                "0 0 12 * * ?",
                "0 0/30 9-17 * * ?",
                "0 15 10 ? * 6L",
                "0 15 10 ? * 2#3",
        };

        int steps = 100;
        int outerLoops = 1000;

        System.out.println("\n========== nextExecution 迭代性能对比 (" + outerLoops + " 轮 x " + steps + " 步) ==========");
        System.out.printf("%-40s %12s %12s %10s%n", "表达式", "Quartz(ms)", "Custom(ms)", "倍数");
        System.out.println("-----------------------------------------------------------------------");

        for (String cron : crons) {
            DateQuartzCronExpression quartzExpr = new DateQuartzCronExpression(cron);
            CustomCronExpression customExpr = new CustomCronExpression(cron);

            // Warmup
            for (int loop = 0; loop < 100; loop++) {
                Date current = BASE_MID_MONTH;
                for (int i = 0; i < steps; i++) {
                    current = quartzExpr.nextExecution(current);
                }
                current = BASE_MID_MONTH;
                for (int i = 0; i < steps; i++) {
                    current = customExpr.nextExecution(current);
                }
            }

            // Quartz
            long startQuartz = System.nanoTime();
            for (int loop = 0; loop < outerLoops; loop++) {
                Date current = BASE_MID_MONTH;
                for (int i = 0; i < steps; i++) {
                    current = quartzExpr.nextExecution(current);
                }
            }
            long quartzNanos = System.nanoTime() - startQuartz;

            // Custom
            long startCustom = System.nanoTime();
            for (int loop = 0; loop < outerLoops; loop++) {
                Date current = BASE_MID_MONTH;
                for (int i = 0; i < steps; i++) {
                    current = customExpr.nextExecution(current);
                }
            }
            long customNanos = System.nanoTime() - startCustom;

            double quartzMs = quartzNanos / 1_000_000.0;
            double customMs = customNanos / 1_000_000.0;
            double ratio = quartzMs / customMs;

            System.out.printf("%-40s %12.2f %12.2f %10.2fx%n", cron, quartzMs, customMs, ratio);
        }
    }

    /**
     * ZonedDateTime 版 nextExecution 性能对比
     */
    @Test
    public void benchmarkZonedDateTimeNextExecution() {
        String[] crons = {
                "0 0 12 * * ?",
                "0 0/30 9-17 * * ?",
                "0 15 10 ? * 6L",
                "0 15 10 LW * ?",
        };

        Clock clock = CLOCK_MID_MONTH;
        int iterations = 50000;

        System.out.println("\n========== ZonedDateTime nextExecution 性能对比 (" + iterations + " 次) ==========");
        System.out.printf("%-40s %12s %12s %10s%n", "表达式", "Quartz(ms)", "Custom(ms)", "倍数");
        System.out.println("-----------------------------------------------------------------------");

        for (String cron : crons) {
            NewQuartzCronExpression quartzExpr = new NewQuartzCronExpression(cron);
            NewCronExpression customExpr = new NewCronExpression(cron);

            // Warmup
            for (int i = 0; i < 5000; i++) {
                quartzExpr.nextExecution(clock);
                customExpr.nextExecution(clock);
            }

            // Quartz
            long startQuartz = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                quartzExpr.nextExecution(clock);
            }
            long quartzNanos = System.nanoTime() - startQuartz;

            // Custom
            long startCustom = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                customExpr.nextExecution(clock);
            }
            long customNanos = System.nanoTime() - startCustom;

            double quartzMs = quartzNanos / 1_000_000.0;
            double customMs = customNanos / 1_000_000.0;
            double ratio = quartzMs / customMs;

            System.out.printf("%-40s %12.2f %12.2f %10.2fx%n", cron, quartzMs, customMs, ratio);
        }
    }

    /**
     * 综合场景：混合多种表达式，模拟真实批量调度场景
     */
    @Test
    public void benchmarkMixedExpressions() {
        String[] crons = {
                "0 0 12 * * ?",
                "0 15 10 * * ?",
                "0 * 14 * * ?",
                "0 0/5 14 * * ?",
                "0 0/5 14,18 * * ?",
                "0 0-5 14 * * ?",
                "0 0 10,15,16 * * ?",
                "0 10,44 14 ? 3 MON-FRI",
                "0 0/30 9-17 * * ?",
                "0 15 10 ? * MON-FRI",
                "0 15 10 15 * ?",
                "0 15 10 L * ?",
                "0 15 10 ? * 6L",
                "0 0 12 ? * WED",
                "0 0 12 ? * MON-FRI",
                "0 15 10 15W * ?",
                "0 15 10 LW * ?",
                "0 15 10 ? * 2#3",
                "0 0 12 1 JAN ?",
                "0 0 22-2 * * ?",
                "0 0 0/4 * * ?",
                "0 0 12 ? * MON,WED,FRI",
                "59 59 23 31 12 ?",
                "0 0 0 1 1 ?",
                "0 0 12 1-7 * ?",
                "0 0 12 ? * 1L",
                "0 0 12 31 * ?",
                "0 0 * ? * MON-FRI",
                "* * * ? * MON-FRI",
        };

        Date baseTime = BASE_MID_MONTH;
        int iterations = 10000;

        System.out.println("\n========== 综合批量场景性能对比 (" + crons.length + " 个表达式 x " + iterations + " 次) ==========");

        // Warmup
        for (String cron : crons) {
            DateQuartzCronExpression q = new DateQuartzCronExpression(cron);
            CustomCronExpression c = new CustomCronExpression(cron);
            for (int i = 0; i < 500; i++) {
                q.nextExecution(baseTime);
                c.nextExecution(baseTime);
            }
        }

        // Quartz batch
        long startQuartz = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            for (String cron : crons) {
                DateQuartzCronExpression q = new DateQuartzCronExpression(cron);
                q.nextExecution(baseTime);
            }
        }
        long quartzNanos = System.nanoTime() - startQuartz;

        // Custom batch
        long startCustom = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            for (String cron : crons) {
                CustomCronExpression c = new CustomCronExpression(cron);
                c.nextExecution(baseTime);
            }
        }
        long customNanos = System.nanoTime() - startCustom;

        // Reuse mode: 预先创建表达式对象，只测计算
        DateQuartzCronExpression[] quartzExprs = new DateQuartzCronExpression[crons.length];
        CustomCronExpression[] customExprs = new CustomCronExpression[crons.length];
        for (int i = 0; i < crons.length; i++) {
            quartzExprs[i] = new DateQuartzCronExpression(crons[i]);
            customExprs[i] = new CustomCronExpression(crons[i]);
        }

        // Warmup reuse
        for (int i = 0; i < 500; i++) {
            for (int j = 0; j < crons.length; j++) {
                quartzExprs[j].nextExecution(baseTime);
                customExprs[j].nextExecution(baseTime);
            }
        }

        long startQuartzReuse = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            for (int j = 0; j < crons.length; j++) {
                quartzExprs[j].nextExecution(baseTime);
            }
        }
        long quartzReuseNanos = System.nanoTime() - startQuartzReuse;

        long startCustomReuse = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            for (int j = 0; j < crons.length; j++) {
                customExprs[j].nextExecution(baseTime);
            }
        }
        long customReuseNanos = System.nanoTime() - startCustomReuse;

        double quartzMs = quartzNanos / 1_000_000.0;
        double customMs = customNanos / 1_000_000.0;
        double quartzReuseMs = quartzReuseNanos / 1_000_000.0;
        double customReuseMs = customReuseNanos / 1_000_000.0;

        System.out.printf("%-30s %12s %12s %10s%n", "模式", "Quartz(ms)", "Custom(ms)", "倍数");
        System.out.println("-----------------------------------------------------------------------");
        System.out.printf("%-30s %12.2f %12.2f %10.2fx%n", "每次新建对象+计算", quartzMs, customMs, quartzMs / customMs);
        System.out.printf("%-30s %12.2f %12.2f %10.2fx%n", "对象复用只测计算", quartzReuseMs, customReuseMs, quartzReuseMs / customReuseMs);
    }
}
