package com.repay.entity.income;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 收益统计响应结果
 */
@Data
public class IncomeResponse {
    /** 统计年份 */
    private int year;
    /** 年度收益汇总 */
    private AnnualIncome annualIncome;
    /** 各月度收益明细 */
    private List<MonthlyIncome> monthlyIncomeList;

    private BigDecimal totalYearIncome;
}