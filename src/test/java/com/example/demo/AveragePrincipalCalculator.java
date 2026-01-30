package com.example.demo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Scanner;

/**
 * 等额本金还款计算器：输出每月明细+每年汇总+总统计
 * 精度保留2位小数（金融计算标准），自动处理四舍五入
 */
public class AveragePrincipalCalculator {
    // 金融计算精度：保留2位小数，四舍五入
    private static final int SCALE = 2;
    private static final RoundingMode ROUND_MODE = RoundingMode.HALF_UP;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        // 1. 输入核心参数（控制台交互式输入，也可直接赋值）
        System.out.print("请输入贷款总额（元）：");
        BigDecimal loanTotal = new BigDecimal(scanner.next()).setScale(SCALE, ROUND_MODE);
        System.out.print("请输入贷款年利率（%，如4.9则输入4.9）：");
        BigDecimal annualRate = new BigDecimal(scanner.next());
        System.out.print("请输入还款年限（年）：");
        int years = scanner.nextInt();
        scanner.close();

        // 2. 计算基础参数
        int totalMonths = years * 12; // 还款总月数
        BigDecimal monthlyPrincipal = loanTotal.divide(new BigDecimal(totalMonths), SCALE+2, ROUND_MODE); // 每月固定本金
        BigDecimal monthlyRate = annualRate.divide(new BigDecimal(1200), 8, ROUND_MODE); // 月利率（保留6位避免精度丢失）
        BigDecimal remainingPrincipal = loanTotal; // 剩余未还本金（初始为贷款总额）

        // 初始化统计变量
        BigDecimal totalAllPrincipal = BigDecimal.ZERO; // 全部期数总还本金
        BigDecimal totalAllInterest = BigDecimal.ZERO; // 全部期数总还利息
        BigDecimal yearPrincipal = BigDecimal.ZERO;    // 当年累计还本金
        BigDecimal yearInterest = BigDecimal.ZERO;     // 当年累计还利息

        // 3. 遍历每月计算明细
        System.out.println("\n==================== 每月还款明细 ====================");
        System.out.printf("%-6s %-12s %-12s %-12s %-12s%n", "期数", "当月本金(元)", "当月利息(元)", "当月还款(元)", "剩余本金(元)");
        for (int month = 1; month <= totalMonths; month++) {
            // 计算当月利息：剩余本金 × 月利率
            BigDecimal monthlyInterest = remainingPrincipal.multiply(monthlyRate).setScale(SCALE, ROUND_MODE);
            // 计算当月还款额
            BigDecimal monthlyRepay = monthlyPrincipal.add(monthlyInterest).setScale(SCALE, ROUND_MODE);
            // 更新剩余本金（剩余本金 - 当月本金）
            remainingPrincipal = remainingPrincipal.subtract(monthlyPrincipal).setScale(SCALE, ROUND_MODE);
            // 防止剩余本金出现负数（精度问题）
            if (remainingPrincipal.compareTo(BigDecimal.ZERO) < 0) {
                remainingPrincipal = BigDecimal.ZERO;
            }

            // 累加当月数据到【当年统计】和【总统计】
            yearPrincipal = yearPrincipal.add(monthlyPrincipal);
            yearInterest = yearInterest.add(monthlyInterest);
            totalAllPrincipal = totalAllPrincipal.add(monthlyPrincipal);
            totalAllInterest = totalAllInterest.add(monthlyInterest);

            // 打印当月明细
            System.out.printf("%-6d %-12.2f %-12.2f %-12.2f %-12.2f%n",
                    month, monthlyPrincipal, monthlyInterest, monthlyRepay, remainingPrincipal);

            // 4. 每年结束时打印【年度汇总】（第12/24/...月）
            if (month % 12 == 0) {
                int currentYear = month / 12;
                BigDecimal yearTotalRepay = yearPrincipal.add(yearInterest);
                System.out.println("-----------------------------------------------------");
                System.out.printf("第%d年汇总：总还本金=%.2f元，总还利息=%.2f元，当年总还款=%.2f元%n",
                        currentYear, yearPrincipal, yearInterest, yearTotalRepay);
                System.out.println("-----------------------------------------------------");
                // 重置当年统计变量，为下一年做准备
                yearPrincipal = BigDecimal.ZERO;
                yearInterest = BigDecimal.ZERO;
            }
        }

        // 5. 打印【全部期数总计】
        BigDecimal totalAllRepay = totalAllPrincipal.add(totalAllInterest);
        System.out.println("==================== 还款总计 ====================");
        System.out.printf("贷款总额：%.2f元，还款年限：%d年（共%d期），年利率：%.2f%%%n",
                loanTotal, years, totalMonths, annualRate);
        System.out.printf("累计总还本金：%.2f元%n", totalAllPrincipal);
        System.out.printf("累计总还利息：%.2f元%n", totalAllInterest);
        System.out.printf("还款总金额：%.2f元%n", totalAllRepay);
    }
}