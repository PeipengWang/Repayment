package com.repay.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

/**
 * 组合贷款还款计算响应
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CombinationLoanResponse extends EqualPrincipalRepayResponse {
    // 商贷明细
    private List<MonthlyDetail> businessMonthlyDetails;
    private List<YearSummary> businessYearSummaries;
    private BigDecimal businessTotalPrincipal;
    private BigDecimal businessTotalInterest;
    private BigDecimal businessTotalRepay;

    // 公积金贷明细
    private List<MonthlyDetail> fundMonthlyDetails;
    private List<YearSummary> fundYearSummaries;
    private BigDecimal fundTotalPrincipal;
    private BigDecimal fundTotalInterest;
    private BigDecimal fundTotalRepay;

    // 合并后总计
    private BigDecimal totalAllPrincipal;    // 总还本金（商+公）
    private BigDecimal totalAllInterest;    // 总还利息（商+公）
    private BigDecimal totalAllRepay;        // 总还款额（商+公）
}