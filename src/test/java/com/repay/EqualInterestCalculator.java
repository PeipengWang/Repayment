package com.repay;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 等额本息还款明细实体类
 * 存储每月还款的核心数据
 */
class EqualInterestDetail {
    // 还款月份
    private int month;
    // 当月应还本金（元，保留2位小数）
    private BigDecimal monthlyPrincipal;
    // 当月应还利息（元，保留2位小数）
    private BigDecimal monthlyInterest;
    // 当月应还总额（元，保留2位小数）
    private BigDecimal monthlyRepay;
    // 当月还款后剩余本金（元，保留2位小数）
    private BigDecimal remainingPrincipal;

    // 全参构造器
    public EqualInterestDetail(int month, BigDecimal monthlyPrincipal, BigDecimal monthlyInterest,
                               BigDecimal monthlyRepay, BigDecimal remainingPrincipal) {
        this.month = month;
        this.monthlyPrincipal = monthlyPrincipal;
        this.monthlyInterest = monthlyInterest;
        this.monthlyRepay = monthlyRepay;
        this.remainingPrincipal = remainingPrincipal;
    }

    // Getter（方便外部获取数据）
    public int getMonth() { return month; }
    public BigDecimal getMonthlyPrincipal() { return monthlyPrincipal; }
    public BigDecimal getMonthlyInterest() { return monthlyInterest; }
    public BigDecimal getMonthlyRepay() { return monthlyRepay; }
    public BigDecimal getRemainingPrincipal() { return remainingPrincipal; }

    // 重写toString，方便打印测试
    @Override
    public String toString() {
        return "第" + month + "月 | 本金：" + monthlyPrincipal + "元 | 利息：" + monthlyInterest +
                "元 | 月供：" + monthlyRepay + "元 | 剩余本金：" + remainingPrincipal + "元";
    }
}

/**
 * 等额本息计算工具类
 * 核心计算逻辑，提供静态方法直接调用，无状态可复用
 */
public class EqualInterestCalculator {
    // 小数位数：金融计算保留2位小数，四舍五入
    private static final int SCALE = 2;
    // 四舍五入模式
    private static final RoundingMode ROUND_MODE = RoundingMode.HALF_UP;

    /**
     * 等额本息核心计算方法
     * @param loanTotal 贷款总额（元，如1000000表示100万）
     * @param annualRate 年利率（%，如4.25表示4.25%）
     * @param loanYears 贷款年限（年，如30表示30年）
     * @return 还款明细列表（包含每月数据，第一个元素为汇总信息，后续为每月明细）
     */
    public static List<EqualInterestDetail> calculate(BigDecimal loanTotal, BigDecimal annualRate, int loanYears, Map<Integer,BigDecimal> prepayMoney) {
        List<EqualInterestDetail> detailList = new ArrayList<>();
        // 1. 基础参数计算
        int totalMonths = loanYears * 12; // 总还款月数
        BigDecimal monthRate = annualRate.divide(new BigDecimal("12"), 8, ROUND_MODE)
                .divide(new BigDecimal("100"), 8, ROUND_MODE); // 月利率，保留8位小数保证精度
        BigDecimal remainingPrincipal = loanTotal; // 初始剩余本金=贷款总额

        // 2. 计算每月固定还款额（核心公式）
        // (1+月利率)^总月数：Math.pow用BigDecimal实现，避免浮点数误差
        BigDecimal powRate = BigDecimal.ONE.add(monthRate).pow(totalMonths);
        // 分子：贷款总额 × 月利率 × (1+月利率)^总月数
        BigDecimal numerator = remainingPrincipal.multiply(monthRate).multiply(powRate);
        // 分母：(1+月利率)^总月数 - 1
        BigDecimal denominator = powRate.subtract(BigDecimal.ONE);
        // 每月固定还款额（保留2位小数）
        BigDecimal monthlyFixedRepay = numerator.divide(denominator, SCALE, ROUND_MODE);
        // 3. 循环计算每月明细
        for (int month = 1; month <= totalMonths; month++) {
            BigDecimal monthlyInterest; // 当月利息
            BigDecimal monthlyPrincipal; // 当月本金
            BigDecimal monthlyRepay = BigDecimal.ZERO; // 当月实际还款额
            BigDecimal finalRemaining; // 当月剩余本金
            if(prepayMoney.containsKey(month)){
                if(remainingPrincipal.compareTo(prepayMoney.get(month)) > 0){
                   remainingPrincipal =  remainingPrincipal.subtract(prepayMoney.get(month));
                   prepayMoney.remove(month);

                }else {
                    prepayMoney.put(month, prepayMoney.get(month).subtract(remainingPrincipal));
                    remainingPrincipal = BigDecimal.ZERO;
                }
                // (1+月利率)^总月数：Math.pow用BigDecimal实现，避免浮点数误差
                powRate = BigDecimal.ONE.add(monthRate).pow(totalMonths - month + 1);
                // 分子：贷款总额 × 月利率 × (1+月利率)^总月数
                numerator = remainingPrincipal.multiply(monthRate).multiply(powRate);
                // 分母：(1+月利率)^总月数 - 1
                denominator = powRate.subtract(BigDecimal.ONE);
                // 每月固定还款额（保留2位小数）
                monthlyFixedRepay = numerator.divide(denominator, SCALE, ROUND_MODE);
            }

            // 计算当月利息：剩余本金 × 月利率（保留2位小数）
            monthlyInterest = remainingPrincipal.multiply(monthRate).setScale(SCALE, ROUND_MODE);

            if (month == totalMonths) {
                // 最后一月：抹平精度误差，剩余本金置0，当月本金=剩余本金，当月还款=本金+利息
                monthlyPrincipal = remainingPrincipal;
                monthlyRepay = monthlyPrincipal.add(monthlyInterest);
                finalRemaining = BigDecimal.ZERO;
            } else {
                // 非最后一月：当月本金=固定还款额-当月利息，剩余本金=上月剩余-当月本金
                monthlyPrincipal = monthlyFixedRepay.subtract(monthlyInterest);
                monthlyRepay = monthlyFixedRepay;
                finalRemaining = remainingPrincipal.subtract(monthlyPrincipal).setScale(SCALE, ROUND_MODE);
            }

            // 添加当月明细到列表
            detailList.add(new EqualInterestDetail(month, monthlyPrincipal, monthlyInterest, monthlyRepay, finalRemaining));
            // 更新剩余本金，用于下月计算
            remainingPrincipal = finalRemaining;
        }

        return detailList;
    }

    /**
     * 计算汇总信息（辅助方法）
     * @param loanTotal 贷款总额
     * @param detailList 还款明细列表
     * @return 汇总数组：[每月固定还款, 总还款额, 总利息, 总月数]
     */
    public static BigDecimal[] calculateSummary(BigDecimal loanTotal, List<EqualInterestDetail> detailList) {
        int totalMonths = detailList.size();
        // 每月固定还款（取第一月的还款额，非最后一月均为固定值）
        BigDecimal monthlyFixedRepay = detailList.get(0).getMonthlyRepay();
        // 总还款额：每月固定还款×总月数 - 第一月固定额 + 最后一月实际额（修正最后一月差异）
        BigDecimal totalRepay = monthlyFixedRepay.multiply(new BigDecimal(totalMonths - 1))
                .add(detailList.get(totalMonths - 1).getMonthlyRepay());
        // 总利息：总还款额 - 贷款总额
        BigDecimal totalInterest = totalRepay.subtract(loanTotal).setScale(SCALE, ROUND_MODE);

        return new BigDecimal[]{monthlyFixedRepay, totalRepay, totalInterest, new BigDecimal(totalMonths)};
    }

    // 测试主方法：直接运行验证计算结果
    public static void main(String[] args) {
        // 测试参数：贷款100万，年利率4.25%，贷款30年
        BigDecimal loanTotal = new BigDecimal("1100000");
        BigDecimal annualRate = new BigDecimal("2.6");
        int loanYears = 30;

        // 执行计算
        Map<Integer, BigDecimal> preMoney = new HashMap<>();
        preMoney.put(12, new BigDecimal(100000));
        List<EqualInterestDetail> detailList = calculate(loanTotal, annualRate, loanYears, preMoney);
        BigDecimal[] summary = calculateSummary(loanTotal, detailList);

        // 打印汇总信息
        System.out.println("======= 等额本息还款汇总 =======");
        System.out.println("贷款总额：" + loanTotal.setScale(SCALE) + " 元");
        System.out.println("贷款年利率：" + annualRate + " %");
        System.out.println("贷款年限：" + loanYears + " 年");
        System.out.println("总还款月数：" + summary[3].intValue() + " 个月");
        System.out.println("每月固定还款：" + summary[0] + " 元");
        System.out.println("还款总本金：" + loanTotal.setScale(SCALE) + " 元");
        System.out.println("还款总利息：" + summary[2] + " 元");
        System.out.println("还款总额（本金+利息）：" + summary[1] + " 元");
        System.out.println("===============================\n");

        // 打印前10个月明细 + 最后1个月明细
        System.out.println("======= 前10个月还款明细 =======");
        for (int i = 0; i < 10; i++) {
            System.out.println(detailList.get(i));
        }
        System.out.println("\n======= 最后1个月还款明细 =======");
        System.out.println(detailList.get(detailList.size() - 1));
    }
}