package com.repay.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
//
    @NotNull(message = "还款年限不能为空")
    @Schema(description = "还款年限", example = "30", required = true)
    private Integer years;

    @NotNull(message = "贷款信息")
    @Schema(description = "贷款信息")
    List<PrincipalRepay> principalRepays;

    @Schema(description = "保留不还金额")
    private BigDecimal reservedPrincipal= BigDecimal.ZERO;

    @Schema(description = "提前还款计划")
    Map<Integer, BigDecimal> prepayMoney = new HashMap<>();
    @Data
    private static class PrincipalRepay{
        @NotNull(message = "贷款总额不能为空")
        @Schema(description = "贷款总额（元）", example = "1000000")
        private BigDecimal loanTotal;

        @NotNull(message = "年利率不能为空")
        @Schema(description = "贷款年利率（%）", example = "4.9")
        private BigDecimal annualRate;
    }
}