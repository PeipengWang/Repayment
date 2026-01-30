package com.repay.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 等额本金还款计算请求参数（含提前还款）
 */
@Data
@Schema(name = "EqualPrincipalRepayRequest", description = "等额本金还款计算请求参数（含提前还款）")
public class EqualPrincipalRepayRequest {
    @NotNull(message = "贷款总额不能为空")
    @Schema(description = "贷款总额（元）", example = "1000000", required = true)
    private BigDecimal loanTotal;

    @NotNull(message = "年利率不能为空")
    @Schema(description = "贷款年利率（%）", example = "4.9", required = true)
    private BigDecimal annualRate;

    @NotNull(message = "还款年限不能为空")
    @Schema(description = "还款年限", example = "30", required = true)
    private Integer years;

    @Schema(description = "还款周期（月，如12表示每年还款）", example = "12", defaultValue = "12")
    private Integer period = 12;

    @Schema(description = "周期额外还款金额（元）", example = "10000")
    private BigDecimal periodPay = BigDecimal.ZERO;

    @Schema(description = "提前还款列表")
    private List<Prepayment> prepayments;

    @Schema(description = "周期性提前还款列表")
    private List<PeriodRepay> periodicRepayList;
}