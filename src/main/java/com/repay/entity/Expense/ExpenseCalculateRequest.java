package com.repay.entity.Expense;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * 支出&应急金计算请求DTO
 */
@Data
public class ExpenseCalculateRequest {
    /** 应急金倍数（N个月） */
    private Integer emergencyMultiple;
    /** 月度必要支出列表 */
    private List<ExpenseItem> monthlyExpenses;
    /** 年度必要支出列表 */
    private List<ExpenseItem> annualExpenses;

    /**
     * 支出项子DTO
     */
    @Data
    public static class ExpenseItem {
        /** 支出名称 */
        private String name;
        /** 支出金额 */
        private BigDecimal amount;
    }
}