package com.repay.entity.income;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 年度收益统计
 */
@Data
public class AnnualIncome {
    /** 年份 */
    private int year;
    /** 年度总收益（元） */
    private BigDecimal totalIncome;

    /** 年度总收入*/
    private BigDecimal totalAllIncome;
    /** 各月度收益明细 */
    private List<MonthlyIncome> monthlyIncomeList;
    /** 各类型年度收益明细 */
    private Map<SavingsType, BigDecimal> typeIncomeMap;

    /** 年度总资产 */
    private BigDecimal totalYearIncome;
}