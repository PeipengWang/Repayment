package com.example.demo.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 还款计算状态类（用于存储计算过程中的临时状态）
 */
@Data
@Schema(name = "RepayCalculationState", description = "还款计算状态")
public class RepayCalculationState {
    // 剩余本金
    private BigDecimal remainingPrincipal;
    // 当前每月应还本金
    private BigDecimal monthlyPrincipal;
    // 已还款总月数
    private Integer paidMonths;
    // 提前还款累计金额
    private BigDecimal totalPrepayAmount;
}