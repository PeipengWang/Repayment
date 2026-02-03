package com.repay;

import com.repay.entity.income.DepositType;
import com.repay.entity.income.IncomeResponse;
import com.repay.entity.income.SavingsProduct;
import com.repay.entity.income.SavingsType;
import com.repay.service.impl.IncomeCalculatorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
public class IncomeCalculatorTest {

    @Autowired
    private IncomeCalculatorService incomeCalculatorService;

    @Test
    public void testCalculateIncome() {
        // 1. 构造测试数据
        List<SavingsProduct> products = new ArrayList<>();

        // 测试1：招商银行活期存款（10万，年利率0.25%，2025-01-01至2025-12-31）
        SavingsProduct demandDeposit = new SavingsProduct();
        demandDeposit.setSavingsType(SavingsType.BANK_DEPOSIT);
        demandDeposit.setProductName("招商银行活期存款");
        demandDeposit.setPrincipal(new BigDecimal("100000"));
        demandDeposit.setAnnualRate(new BigDecimal("0.25"));
        demandDeposit.setStartDate(LocalDate.of(2025, 1, 1));
        demandDeposit.setEndDate(LocalDate.of(2025, 12, 31));
        demandDeposit.setDepositType(DepositType.DEMAND);
        products.add(demandDeposit);

        // 测试2：工商银行定期存款（20万，年利率2.75%，2024-06-01至2025-06-01，到期一次性计息）
        SavingsProduct fixedDeposit = new SavingsProduct();
        fixedDeposit.setSavingsType(SavingsType.BANK_DEPOSIT);
        fixedDeposit.setProductName("工商银行1年期定期存款");
        fixedDeposit.setPrincipal(new BigDecimal("200000"));
        fixedDeposit.setAnnualRate(new BigDecimal("2.75"));
        fixedDeposit.setStartDate(LocalDate.of(2024, 6, 1));
        fixedDeposit.setEndDate(LocalDate.of(2025, 6, 1));
        fixedDeposit.setDepositType(DepositType.FIXED);
        products.add(fixedDeposit);

        // 测试3：国债（5万，年利率3.0%，2025-01-01至2025-12-31，到期一次性计息）
        SavingsProduct bond = new SavingsProduct();
        bond.setSavingsType(SavingsType.BOND);
        bond.setProductName("2025年国债");
        bond.setPrincipal(new BigDecimal("50000"));
        bond.setAnnualRate(new BigDecimal("3.0"));
        bond.setStartDate(LocalDate.of(2025, 1, 1));
        bond.setEndDate(LocalDate.of(2025, 12, 31));
        products.add(bond);

        // 测试4：易方达基金（8万，月度收益率0.8%，2025-01-01至2025-12-31）
        SavingsProduct fund = new SavingsProduct();
        fund.setSavingsType(SavingsType.FUND);
        fund.setProductName("易方达沪深300基金");
        fund.setPrincipal(new BigDecimal("80000"));
        fund.setMonthlyRate(new BigDecimal("0.8"));
        fund.setStartDate(LocalDate.of(2025, 1, 1));
        fund.setEndDate(LocalDate.of(2025, 12, 31));
        products.add(fund);

        // 2. 计算2025年收益
        IncomeResponse response = incomeCalculatorService.calculateIncome(products, 2025);

        // 3. 打印结果
        System.out.println("===== 2025年收益统计 =====");
        System.out.println("年度总收益：" + response.getAnnualIncome().getTotalIncome() + " 元");
        System.out.println("各类型年度收益：");
        response.getAnnualIncome().getTypeIncomeMap().forEach((type, income) -> {
            System.out.println(type.getDesc() + "：" + income + " 元");
        });

        System.out.println("\n===== 月度收益明细 =====");
        response.getMonthlyIncomeList().forEach(monthly -> {
            System.out.println(monthly.getYear() + "年" + monthly.getMonth() + "月：");
            System.out.println("  总收益：" + monthly.getTotalIncome() + " 元");
            monthly.getTypeIncomeMap().forEach((type, income) -> {
                System.out.println("  " + type.getDesc() + "：" + income + " 元");
            });
        });
    }
}