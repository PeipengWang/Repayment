package com.repay.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 组合贷款（商贷+公积金）请求参数
 */
@Data
@Schema(description = "组合贷款还款计算请求参数")
public class CombinationLoanRequest {
    @Schema(description = "贷款类型：single(纯商贷)、fund(纯公积金)、combination(组合贷)", example = "combination")
    @NotBlank(message = "贷款类型不能为空")
    private String loanType;

    // 商贷参数
    @Schema(description = "商贷总额（元）", example = "1000000")
    @Min(value = 0, message = "商贷总额不能为负数")
    private BigDecimal businessLoanTotal = BigDecimal.ZERO;

    @Schema(description = "商贷年利率（%）", example = "4.9")
    @Min(value = 0, message = "商贷年利率不能为负数")
    private BigDecimal businessAnnualRate = BigDecimal.ZERO;

    @Schema(description = "商贷还款年限", example = "30")
    @Min(value = 0, message = "商贷还款年限不能小于1")
    private Integer businessYears = 0;

    // 公积金贷参数
    @Schema(description = "公积金贷总额（元）", example = "500000")
    @Min(value = 0, message = "公积金贷总额不能为负数")
    private BigDecimal fundLoanTotal = BigDecimal.ZERO;

    @Schema(description = "公积金贷年利率（%）", example = "3.1")
    @Min(value = 0, message = "公积金贷年利率不能为负数")
    private BigDecimal fundAnnualRate = BigDecimal.ZERO;

    @Schema(description = "公积金贷还款年限", example = "30")
    @Min(value = 0, message = "公积金贷还款年限不能小于1")
    private Integer fundYears = 0;

    // 公共参数
    @Schema(description = "保留本金（元）", example = "0")
    @Min(value = 0, message = "保留本金不能为负数")
    private BigDecimal reservedPrincipal = BigDecimal.ZERO;

    @Schema(description = "还款周期（月）", example = "1")
    private Integer period = 1;

    @Schema(description = "提前还款列表")
    @Valid
    private List<Prepayment> prepayments = List.of();

    @Schema(description = "周期性还款列表")
    @Valid
    private List<PeriodRepay> periodicRepayList = List.of();
}