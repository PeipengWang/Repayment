package com.repay.controller;

import com.repay.entity.Expense.ExpenseCalculateRequest;
import com.repay.entity.Expense.ExpenseCalculateResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 必要支出&应急金计算控制器
 */
@RestController
@RequestMapping("/api/expense")
public class ExpenseController {

    /**
     * 计算应急金
     * @param request 支出计算请求参数
     * @return 应急金计算结果
     */
    @PostMapping("/calculateEmergencyFund")
    public ExpenseCalculateResponse calculateEmergencyFund(@RequestBody ExpenseCalculateRequest request) {
        // 1. 校验参数
        if (request.getEmergencyMultiple() == null || request.getEmergencyMultiple() < 1 ) {
            throw new IllegalArgumentException("应急金倍数大于1个月");
        }
        if (request.getMonthlyExpenses() == null || request.getAnnualExpenses() == null) {
            throw new IllegalArgumentException("支出数据不能为空");
        }

        // 2. 计算月度支出总计
        BigDecimal monthlyTotal = calculateTotalAmount(request.getMonthlyExpenses());
        // 3. 计算年度支出总计
        BigDecimal annualTotal = calculateTotalAmount(request.getAnnualExpenses());
        // 4. 计算应急金：月度支出 × 倍数 + 年度支出
        BigDecimal emergencyFund = monthlyTotal.multiply(new BigDecimal(request.getEmergencyMultiple()))
                .add(annualTotal)
                .setScale(2, RoundingMode.HALF_UP); // 保留2位小数，四舍五入


        // 5. 构造响应结果
        ExpenseCalculateResponse response = new ExpenseCalculateResponse();
        response.setMonthlyTotal(monthlyTotal);
        response.setAnnualTotal(annualTotal);
        response.setEmergencyFund(emergencyFund);

        return response;
    }

    /**
     * 计算支出列表的总金额
     * @param expenseItems 支出项列表
     * @return 总金额
     */
    private BigDecimal calculateTotalAmount(List<ExpenseCalculateRequest.ExpenseItem> expenseItems) {
        BigDecimal total = BigDecimal.ZERO;
        for (ExpenseCalculateRequest.ExpenseItem item : expenseItems) {
            if (item.getAmount() != null) {
                total = total.add(item.getAmount());
            }
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }
}