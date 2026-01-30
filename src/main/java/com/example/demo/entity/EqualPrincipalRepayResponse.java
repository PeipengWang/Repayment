package com.example.demo.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.List;

/**
 * 等额本金还款计算结果响应
 */
@Data
@Schema(name = "EqualPrincipalRepayResponse", description = "等额本金还款计算结果（含提前还款）")
@Accessors(chain = true)
public class EqualPrincipalRepayResponse {
    // 基础参数
    @Schema(description = "贷款总额（元）")
    private BigDecimal loanTotal;
    @Schema(description = "贷款年利率（%）")
    private BigDecimal annualRate;
    @Schema(description = "还款年限")
    private Integer years;
    @Schema(description = "总还款月数")
    private Integer totalMonths;

    // 每月明细
    @Schema(description = "每月还款明细列表")
    private List<MonthlyDetail> monthlyDetails;

    // 年度汇总
    @Schema(description = "每年还款汇总列表")
    private List<YearSummary> yearSummaries;

    // 总计信息
    @Schema(description = "累计总还本金（元）")
    private BigDecimal totalAllPrincipal;
    @Schema(description = "累计总还利息（元）")
    private BigDecimal totalAllInterest;
    @Schema(description = "还款总金额（元）")
    private BigDecimal totalAllRepay;

    /**
     * 每月还款明细
     */
    @Data
    @Schema(name = "MonthlyDetail", description = "每月还款明细")
    public static class MonthlyDetail {
        @Schema(description = "期数（第N个月）")
        private Integer month;
        @Schema(description = "当月本金（元，含提前还款）")
        private BigDecimal monthlyPrincipal;
        @Schema(description = "当月利息（元）")
        private BigDecimal monthlyInterest;
        @Schema(description = "当月还款额（元）")
        private BigDecimal monthlyRepay;
        @Schema(description = "剩余本金（元）")
        private BigDecimal remainingPrincipal;
    }

    /**
     * 每年还款汇总
     */
    @Data
    @Schema(name = "YearSummary", description = "每年还款汇总")
    public static class YearSummary {
        @Schema(description = "年份（第N年）")
        private Integer year;
        @Schema(description = "当年总还本金（元）")
        private BigDecimal yearPrincipal;
        @Schema(description = "当年总还利息（元）")
        private BigDecimal yearInterest;
        @Schema(description = "当年总还款（元）")
        private BigDecimal yearTotalRepay;
    }
}