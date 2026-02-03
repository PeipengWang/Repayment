package com.repay.service.impl;

import com.repay.entity.income.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 收益计算核心服务
 */
@Service
public class IncomeCalculatorService {
    // 计算精度：保留2位小数，四舍五入
    private static final int SCALE = 2;
    private static final RoundingMode ROUND_MODE = RoundingMode.HALF_UP;

    /**
     * 核心计算方法：统计指定年份的月度/年度收益
     * @param products 储蓄产品列表
     * @param targetYear 统计年份（如2025）
     * @return 收益统计结果
     */
    public IncomeResponse calculateIncome(List<SavingsProduct> products, int targetYear) {
        // 1. 初始化月度收益容器（1-12月）
        Map<Integer, MonthlyIncome> monthlyIncomeMap = new HashMap<>();
        for (int month = 1; month <= 12; month++) {
            MonthlyIncome monthlyIncome = new MonthlyIncome();
            monthlyIncome.setYear(targetYear);
            monthlyIncome.setMonth(month);
            monthlyIncome.setTotalIncome(BigDecimal.ZERO);
            monthlyIncome.setTypeIncomeMap(new EnumMap<>(SavingsType.class));
            monthlyIncomeMap.put(month, monthlyIncome);
        }

        BigDecimal totalYear = BigDecimal.ZERO;
        // 2. 遍历每个产品，计算对应收益
        for (SavingsProduct product : products) {
            totalYear = totalYear.add(product.getPrincipal());
            switch (product.getSavingsType()) {
                case BANK_DEPOSIT:
                    calculateBankDepositIncome(product, targetYear, monthlyIncomeMap);
                    break;
                case BOND:
                    calculateBondIncome(product, targetYear, monthlyIncomeMap);
                    break;
                case FUND:
                    calculateFundIncome(product, targetYear, monthlyIncomeMap);
                    break;
                case STOCK:
                    calculateStockIncome(product, targetYear, monthlyIncomeMap);
                    break;
                default:
                    throw new IllegalArgumentException("不支持的储蓄类型：" + product.getSavingsType());
            }
        }

        // 3. 汇总年度收益
        AnnualIncome annualIncome = summarizeAnnualIncome(targetYear, monthlyIncomeMap, totalYear);

        // 4. 封装响应结果
        IncomeResponse response = new IncomeResponse();
        response.setYear(targetYear);
        response.setAnnualIncome(annualIncome);
        response.setMonthlyIncomeList(monthlyIncomeMap.values().stream()
                .sorted(Comparator.comparingInt(MonthlyIncome::getMonth))
                .collect(Collectors.toList()));

        return response;
    }

    /**
     * 计算银行存款收益（活期/定期）
     */
    private void calculateBankDepositIncome(SavingsProduct product, int targetYear, Map<Integer, MonthlyIncome> monthlyIncomeMap) {
        BigDecimal principal = product.getPrincipal();
        BigDecimal annualRate = product.getAnnualRate();
        LocalDate startDate = product.getStartDate();
        LocalDate endDate = product.getEndDate();
        DepositType depositType = product.getDepositType();

        // 过滤：产品起始日期晚于统计年，或结束日期早于统计年（无收益）
        LocalDate targetYearStart = LocalDate.of(targetYear, 1, 1);
        LocalDate targetYearEnd = LocalDate.of(targetYear, 12, 31);
        if (startDate.isAfter(targetYearEnd) ) {
            return;
        }

        if (DepositType.DEMAND.equals(depositType)) {
            // 活期存款：按日计息，每日收益=本金*年利率/365
            BigDecimal dailyRate = annualRate.divide(new BigDecimal(365), SCALE + 2, ROUND_MODE);
            BigDecimal dailyIncome = principal.multiply(dailyRate).divide(new BigDecimal(100), SCALE, ROUND_MODE);
            // 遍历统计年的每个月，计算当月天数及收益
            for (int month = 1; month <= 12; month++) {
                LocalDate monthStart = LocalDate.of(targetYear, month, 1);
                LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);

                // 计算产品在当月的有效天数（起始日期≤当天≤结束日期/当月末）
                LocalDate actualStart = startDate.isAfter(monthStart) ? startDate : monthStart;
                LocalDate actualEnd = (endDate != null && endDate.isBefore(monthEnd)) ? endDate : monthEnd;

                if (actualStart.isAfter(actualEnd)) {
                    continue; // 当月无有效天数
                }

                long days = ChronoUnit.DAYS.between(actualStart, actualEnd) + 1;
                BigDecimal monthIncome = dailyIncome.multiply(new BigDecimal(days)).setScale(SCALE, ROUND_MODE);
                // 累加至当月收益
                addIncomeToMonth(monthlyIncomeMap, targetYear, month, SavingsType.BANK_DEPOSIT, monthIncome);
                //每3个月将利息追加到本金上
            }
        } else if (DepositType.FIXED.equals(depositType)) {
            // 定期存款：到期一次性计息，收益=本金*年利率*存期（年）
            if (endDate == null) {
                throw new IllegalArgumentException("定期存款必须设置到期日期");
            }

            // 仅到期日在统计年时，计算收益
            if (endDate.getYear() == targetYear) {
                int endMonth = endDate.getMonthValue();
                // 计算存期（年）：精确到天
                long days = ChronoUnit.DAYS.between(startDate, endDate);
                BigDecimal periodYears = new BigDecimal(days).divide(new BigDecimal(365), SCALE + 2, ROUND_MODE);
                BigDecimal totalIncome = principal.multiply(annualRate).multiply(periodYears)
                        .divide(new BigDecimal(100), SCALE, ROUND_MODE);
                // 收益计入到期当月
                addIncomeToMonth(monthlyIncomeMap, targetYear, endMonth, SavingsType.BANK_DEPOSIT, totalIncome);
            }
        }
    }

    /**
     * 计算债券收益（参考定期存款，到期一次性计息）
     */
    private void calculateBondIncome(SavingsProduct product, int targetYear, Map<Integer, MonthlyIncome> monthlyIncomeMap) {
        BigDecimal principal = product.getPrincipal();
        BigDecimal annualRate = product.getAnnualRate();
        LocalDate startDate = product.getStartDate();
        LocalDate endDate = product.getEndDate();

        if (endDate == null) {
            throw new IllegalArgumentException("债券必须设置到期日期");
        }

        // 仅到期日在统计年时计算收益
        if (endDate.getYear() == targetYear) {
            int endMonth = endDate.getMonthValue();
            long days = ChronoUnit.DAYS.between(startDate, endDate);
            BigDecimal periodYears = new BigDecimal(days).divide(new BigDecimal(365), SCALE + 2, ROUND_MODE);
            BigDecimal totalIncome = principal.multiply(annualRate).multiply(periodYears)
                    .divide(new BigDecimal(100), SCALE, ROUND_MODE);

            addIncomeToMonth(monthlyIncomeMap, targetYear, endMonth, SavingsType.BOND, totalIncome);
        }
    }

    /**
     * 计算基金收益（按月度收益率计息，每月结算）
     */
    private void calculateFundIncome(SavingsProduct product, int targetYear, Map<Integer, MonthlyIncome> monthlyIncomeMap) {
        BigDecimal principal = product.getPrincipal();
        // 优先使用月度收益率，无则按年利率/12计算
        BigDecimal monthlyRate = product.getMonthlyRate() != null && !product.getMonthlyRate().equals(BigDecimal.ZERO)? product.getMonthlyRate()
                : product.getAnnualRate().divide(new BigDecimal(12), SCALE + 2, ROUND_MODE);

        LocalDate startDate = product.getStartDate();
        LocalDate endDate = product.getEndDate();

        // 遍历统计年的每个月
        for (int month = 1; month <= 12; month++) {
            LocalDate monthEnd = LocalDate.of(targetYear, month, 1).plusMonths(1).minusDays(1);
            // 产品未开始/已结束，跳过
            if (startDate.isAfter(monthEnd) || (endDate != null && endDate.isBefore(LocalDate.of(targetYear, month, 1)))) {
                continue;
            }

            BigDecimal monthIncome = principal.multiply(monthlyRate).divide(new BigDecimal(100), SCALE, ROUND_MODE);
            addIncomeToMonth(monthlyIncomeMap, targetYear, month, SavingsType.FUND, monthIncome);
        }
    }

    /**
     * 计算股票收益（按月度收益率计息，每月结算，可扩展涨跌逻辑）
     */
    private void calculateStockIncome(SavingsProduct product, int targetYear, Map<Integer, MonthlyIncome> monthlyIncomeMap) {
        BigDecimal principal = product.getPrincipal();
        BigDecimal monthlyRate = product.getMonthlyRate() != null ? product.getMonthlyRate()
                : product.getAnnualRate().divide(new BigDecimal(12), SCALE + 2, ROUND_MODE);

        LocalDate startDate = product.getStartDate();
        LocalDate endDate = product.getEndDate();

        // 遍历统计年的每个月
        for (int month = 1; month <= 12; month++) {
            LocalDate monthEnd = LocalDate.of(targetYear, month, 1).plusMonths(1).minusDays(1);
            if (startDate.isAfter(monthEnd) || (endDate != null && endDate.isBefore(LocalDate.of(targetYear, month, 1)))) {
                continue;
            }

            BigDecimal monthIncome = principal.multiply(monthlyRate).divide(new BigDecimal(100), SCALE, ROUND_MODE);
            addIncomeToMonth(monthlyIncomeMap, targetYear, month, SavingsType.STOCK, monthIncome);
        }
    }

    /**
     * 累加收益到指定月份
     */
    private void addIncomeToMonth(Map<Integer, MonthlyIncome> monthlyIncomeMap, int year, int month,
                                  SavingsType type, BigDecimal income) {
        MonthlyIncome monthlyIncome = monthlyIncomeMap.get(month);
        // 累加类型收益
        BigDecimal oldTypeIncome = monthlyIncome.getTypeIncomeMap().getOrDefault(type, BigDecimal.ZERO);
        monthlyIncome.getTypeIncomeMap().put(type, oldTypeIncome.add(income).setScale(SCALE, ROUND_MODE));
        // 累加月度总收益
        monthlyIncome.setTotalIncome(monthlyIncome.getTotalIncome().add(income).setScale(SCALE, ROUND_MODE));
    }

    /**
     * 汇总年度收益
     */
    private AnnualIncome summarizeAnnualIncome(int year, Map<Integer, MonthlyIncome> monthlyIncomeMap, BigDecimal totalYearPrincipal) {
        AnnualIncome annualIncome = new AnnualIncome();
        annualIncome.setYear(year);
        annualIncome.setTotalIncome(BigDecimal.ZERO);
        annualIncome.setTypeIncomeMap(new EnumMap<>(SavingsType.class));

        // 汇总各类型年度收益 + 年度总收益
        for (MonthlyIncome monthly : monthlyIncomeMap.values()) {
            // 累加总收益
            annualIncome.setTotalIncome(annualIncome.getTotalIncome().add(monthly.getTotalIncome()));
            // 累加各类型收益
            for (Map.Entry<SavingsType, BigDecimal> entry : monthly.getTypeIncomeMap().entrySet()) {
                BigDecimal old = annualIncome.getTypeIncomeMap().getOrDefault(entry.getKey(), BigDecimal.ZERO);
                annualIncome.getTypeIncomeMap().put(entry.getKey(), old.add(entry.getValue()).setScale(SCALE, ROUND_MODE));
            }
        }
        annualIncome.setTotalYearIncome(totalYearPrincipal.add(annualIncome.getTotalIncome()));
        // 整理月度收益列表
        annualIncome.setMonthlyIncomeList(monthlyIncomeMap.values().stream()
                .sorted(Comparator.comparingInt(MonthlyIncome::getMonth))
                .collect(Collectors.toList()));

        return annualIncome;
    }
}