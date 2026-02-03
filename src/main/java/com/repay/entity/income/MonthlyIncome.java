package com.repay.entity.income;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 月度收益统计
 */
@Data
public class MonthlyIncome {
    /** 年份（如2025） */
    private int year;
    /** 月份（1-12） */
    private int month;
    /** 总收益（元） */
    private BigDecimal totalIncome;
    /** 各类型收益明细（key：储蓄类型，value：收益金额） */
    private Map<SavingsType, BigDecimal> typeIncomeMap;
}