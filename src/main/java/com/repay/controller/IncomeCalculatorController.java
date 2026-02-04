package com.repay.controller;

import com.repay.entity.income.IncomeCalculateRequest;
import com.repay.entity.income.IncomeResponse;
import com.repay.entity.income.SavingsProduct;
import com.repay.service.impl.IncomeCalculatorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 收益统计接口
 */
@RestController
@RequestMapping("/api/income")
@Tag(name = "收益统计接口", description = "月度/年度收益统计（支持银行存款、债券、基金、股票）")
public class IncomeCalculatorController {

    @Autowired
    private IncomeCalculatorService incomeCalculatorService;

    /**
     * 统计指定年份的收益
     * @param targetYear 统计年份（如2025）
     * @return 收益统计结果
     */
    @PostMapping("/calculate")
    @Operation(summary = "收益统计", description = "计算指定年份的月度/年度收益，支持多储蓄类型")
    public IncomeResponse calculateIncome(
            @RequestBody IncomeCalculateRequest incomeCalculateRequest ,
            @RequestParam int targetYear) {

        return incomeCalculatorService.calculateIncome(incomeCalculateRequest, targetYear);
    }
}