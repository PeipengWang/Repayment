package com.repay.entity.EmergencyFund;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 应急金计算请求参数
 */
@Data
@Schema(description = "应急金计算请求参数")
public class EmergencyFundRequest {
    @Schema(description = "月度应急金额", example = "5000")
    @NotNull(message = "月度应急金额")
    @Min(value = 0, message = "月度应急金额不能为负数")
    private BigDecimal monthlyEmergencyQuota;

    @Schema(description = "应急金倍数N（通常为3-6）", example = "6")
    @NotNull(message = "应急金倍数不能为空")
    @Min(value = 1, message = "应急金倍数不能小于1")
    private Integer multipleN;

    @Schema(description = "每月必要支出", example = "100000")
    @NotNull(message = "每月必要支出不能为空")
    @Min(value = 0, message = "每月必要支出不能为负数")
    private BigDecimal monthlyNecessaryExpense;

    @Schema(description = "年度支出", example = "100000")
    @NotNull(message = "年度支出不能为空")
    @Min(value = 0, message = "年度其不能为负数")
    private BigDecimal annualExpense;

    @Schema(description = "当前剩余存款", example = "100000")
    @NotNull(message = "当前剩余存款不能为空")
    @Min(value = 0, message = "当前剩余存款不能为负数")
    private BigDecimal currentDeposit;

    @Schema(description = "存款年利率（%）", example = "1.5")
    private BigDecimal depositInterestRate = BigDecimal.ZERO;

    @Schema(description = "剩余贷款额度", example = "500000")
    private BigDecimal remainingLoan = BigDecimal.ZERO;

    @Schema(description = "LPR利率（%）", example = "3.85")
    private BigDecimal lprRate = BigDecimal.ZERO;

    @Schema(description = "还款方式（等额本金/等额本息）", example = "等额本息")
    private String repayMethod;

    @Schema(description = "还款年限", example = "20")
    private Integer repayYears = 0;

    @Schema(description = "月份工资收入", example = "15000")
    @NotNull(message = "月份工资收入不能为空")
    @Min(value = 0, message = "月份工资收入不能为负数")
    private BigDecimal monthlyIncome;

    @Schema(description = "其他收入", example = "2000")
    private BigDecimal otherIncome = BigDecimal.ZERO;

    @Schema(description = "统计起始时间", example = "2026-02-01")
    @NotNull(message = "统计起始时间不能为空")
    private LocalDate startDate;

    @Schema(description = "统计时长（月）", example = "12")
    @NotNull(message = "统计时长不能为空")
    @Min(value = 1, message = "统计时长不能小于1")
    private Integer statMonths;
}