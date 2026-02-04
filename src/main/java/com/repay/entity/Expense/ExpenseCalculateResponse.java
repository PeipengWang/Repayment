package com.repay.entity.Expense;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 支出&应急金计算响应DTO
 */
@Data
public class ExpenseCalculateResponse {
    /** 月度支出总计 */
    private BigDecimal monthlyTotal;
    /** 年度支出总计 */
    private BigDecimal annualTotal;
    /** 应急金总额 */
    private BigDecimal emergencyFund;
}