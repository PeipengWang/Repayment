package com.repay.controller;

import com.repay.entity.EmergencyFund.AnnualStatDTO;
import com.repay.entity.EmergencyFund.EmergencyFundRequest;
import com.repay.entity.EmergencyFund.EmergencyFundResponse;
import com.repay.entity.EmergencyFund.MonthlyStatDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 应急金计算控制器
 */
@RestController
@RequestMapping("/api/emergency")
@Tag(name = "应急金计算接口", description = "家庭应急金、抗风险能力计算")
@Slf4j
public class EmergencyFundController {

    /**
     * 计算应急金相关数据
     */
    @PostMapping("/calculate")
    @Operation(summary = "应急金计算", description = "计算单月应急额度、覆盖月数、年度平均应急能力等")
    public EmergencyFundResponse calculate(
            @Parameter(description = "应急金计算参数")
            @Validated @RequestBody EmergencyFundRequest request) {

        log.info("开始计算应急金，参数：{}", request);
        EmergencyFundResponse response = new EmergencyFundResponse();

        // ========== 核心计算步骤 ==========
        // 1. 计算单月应急金额度 = (每月必要支出*12 + 年度支出)/12
        BigDecimal monthlyEmergencyQuota = request.getMonthlyNecessaryExpense()
                .multiply(new BigDecimal(12))
                .add(request.getAnnualExpense())
                .divide(new BigDecimal(12), 2, RoundingMode.HALF_UP);

        // 2. 基础应急能力金额额度 = 单月应急额度 * N
        BigDecimal baseEmergencyQuota = monthlyEmergencyQuota
                .multiply(new BigDecimal(request.getMultipleN()))
                .setScale(2, RoundingMode.HALF_UP);

        // 3. 计算贷款月供（简化版，实际可根据还款方式精准计算）
        BigDecimal monthlyLoanRepay = calculateMonthlyLoanRepay(request);

        // 4. 构建月度统计数据
        List<MonthlyStatDTO> monthlyStatList = new ArrayList<>();
        BigDecimal totalCoverageMonths = BigDecimal.ZERO; // 总覆盖月数
        BigDecimal totalAnnualBalance = BigDecimal.ZERO; // 年度总余额

        LocalDate currentMonth = request.getStartDate();
        for (int i = 0; i < request.getStatMonths(); i++) {
            MonthlyStatDTO monthlyStat = new MonthlyStatDTO();
            monthlyStat.setStatMonth(currentMonth);

            // 每月总收入 = 工资 + 其他收入
            BigDecimal monthlyIncome = request.getMonthlyIncome()
                    .add(request.getOtherIncome())
                    .setScale(2, RoundingMode.HALF_UP);
            monthlyStat.setMonthlyIncome(monthlyIncome);

            // 每月总支出 = 必要支出 + 贷款月供
            BigDecimal monthlyExpense = request.getMonthlyEmergencyQuota()
                    .add(monthlyLoanRepay)
                    .setScale(2, RoundingMode.HALF_UP);
            monthlyStat.setMonthlyExpense(monthlyExpense);

            // 月度余额 = 收入 - 支出
            BigDecimal monthlyBalance = monthlyIncome.subtract(monthlyExpense)
                    .setScale(2, RoundingMode.HALF_UP);
            monthlyStat.setMonthlyBalance(monthlyBalance);
            totalAnnualBalance = totalAnnualBalance.add(monthlyBalance);

            // 月度应急覆盖月数 = 当前存款 / 单月应急额度（简化版，实际可按每月存款变化调整）
            BigDecimal coverageMonths = request.getCurrentDeposit()
                    .divide(monthlyEmergencyQuota, 2, RoundingMode.HALF_UP);
            monthlyStat.setEmergencyCoverageMonths(coverageMonths);
            totalCoverageMonths = totalCoverageMonths.add(coverageMonths);

            monthlyStatList.add(monthlyStat);
            // 月份+1
            currentMonth = currentMonth.plusMonths(1);
        }

        // 5. 年度统计
        AnnualStatDTO annualStat = new AnnualStatDTO();
        annualStat.setMonthlyEmergencyQuota(monthlyEmergencyQuota);
        annualStat.setBaseEmergencyQuota(baseEmergencyQuota);

        // 年度平均应急能力 = 总覆盖月数 / 12
        BigDecimal annualAvgEmergencyAbility = totalCoverageMonths
                .divide(new BigDecimal(12), 2, RoundingMode.HALF_UP);
        annualStat.setAnnualAvgEmergencyAbility(annualAvgEmergencyAbility);

        // 年度剩余资金 = 年度总余额 + 存款利息
        BigDecimal depositInterest = request.getCurrentDeposit()
                .multiply(request.getDepositInterestRate().divide(new BigDecimal(100), 4, RoundingMode.HALF_UP))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal annualRemainingFund = totalAnnualBalance.add(depositInterest)
                .setScale(2, RoundingMode.HALF_UP);
        annualStat.setAnnualRemainingFund(annualRemainingFund);

        // ========== 组装响应 ==========
        response.setAnnualStat(annualStat);
        response.setMonthlyStatList(monthlyStatList);

        log.info("应急金计算完成，结果：{}", response);
        return response;
    }

    /**
     * 简化版贷款月供计算（实际项目可替换为精准算法）
     */
    private BigDecimal calculateMonthlyLoanRepay(EmergencyFundRequest request) {
        if (request.getRemainingLoan().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal monthlyRate = request.getLprRate()
                .divide(new BigDecimal(100), 4, RoundingMode.HALF_UP)
                .divide(new BigDecimal(12), 4, RoundingMode.HALF_UP);
        int totalMonths = request.getRepayYears() * 12;

        BigDecimal monthlyRepay;
        if ("等额本金".equals(request.getRepayMethod())) {
            // 等额本金：每月应还本金 = 贷款总额/总月数；每月利息 = 剩余本金*月利率
            BigDecimal monthlyPrincipal = request.getRemainingLoan()
                    .divide(new BigDecimal(totalMonths), 2, RoundingMode.HALF_UP);
            BigDecimal firstMonthInterest = request.getRemainingLoan()
                    .multiply(monthlyRate)
                    .setScale(2, RoundingMode.HALF_UP);
            monthlyRepay = monthlyPrincipal.add(firstMonthInterest); // 取首月还款额简化
        } else {
            // 等额本息：月供 = [贷款本金×月利率×(1+月利率)^还款月数]÷[(1+月利率)^还款月数－1]
            BigDecimal pow = new BigDecimal(Math.pow(1 + monthlyRate.doubleValue(), totalMonths));
            monthlyRepay = request.getRemainingLoan()
                    .multiply(monthlyRate)
                    .multiply(pow)
                    .divide(pow.subtract(BigDecimal.ONE), 2, RoundingMode.HALF_UP);
        }

        return monthlyRepay.setScale(2, RoundingMode.HALF_UP);
    }
}