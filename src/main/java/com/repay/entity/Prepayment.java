package com.repay.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 提前还款信息
 */
@Data
@Schema(name = "Prepayment", description = "提前还款信息")
public class Prepayment {
    @Schema(description = "提前还款月份（第N个月）", example = "12", required = true)
    private Integer month;

    @Schema(description = "提前还款金额（元）", example = "50000", required = true)
    private BigDecimal amount;
}