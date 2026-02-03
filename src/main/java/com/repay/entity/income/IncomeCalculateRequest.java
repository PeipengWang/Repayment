package com.repay.entity.income;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

// 后端接收参数的DTO示例
@Data
public class IncomeCalculateRequest {
    private List<SavingsProduct> products; // 原有产品列表
    private SalaryConfig salaryConfig;     // 新增工资配置

    // getter/setter
    public static class SalaryConfig {
        private Integer salaryDay;        // 每月发薪日
        private BigDecimal monthlySalary; // 每月工资金额
    }
}