//package com.repay;
//
//import java.math.BigDecimal;
//import java.math.RoundingMode;
//import java.util.ArrayList;
//import java.util.List;
//
///**
// * 等额本息还款明细实体类（含提前还款标记）
// */
//class EqualInterestDetail2 {
//    // 还款月份
//    private int month;
//    // 当月应还本金（元，保留2位小数）
//    private BigDecimal monthlyPrincipal;
//    // 当月应还利息（元，保留2位小数）
//    private BigDecimal monthlyInterest;
//    // 当月提前还款本金（0表示无提前还款）
//    private BigDecimal prepayPrincipal;
//    // 当月实际总还款额（元，保留2位小数）
//    private BigDecimal actualRepay;
//    // 当月还款后剩余本金（元，保留2位小数）
//    private BigDecimal remainingPrincipal;
//    // 是否为提前还款月份
//    private boolean isPrepay;
//
//    // 全参构造器（普通还款）
//    public EqualInterestDetail2(int month, BigDecimal monthlyPrincipal, BigDecimal monthlyInterest,
//                                BigDecimal actualRepay, BigDecimal remainingPrincipal) {
//        this(month, monthlyPrincipal, monthlyInterest, BigDecimal.ZERO, actualRepay, remainingPrincipal, false);
//    }
//
//    // 全参构造器（提前还款）
//    public EqualInterestDetail2(int month, BigDecimal monthlyPrincipal, BigDecimal monthlyInterest,
//                                BigDecimal prepayPrincipal, BigDecimal actualRepay, BigDecimal remainingPrincipal,
//                                boolean isPrepay) {
//        this.month = month;
//        this.monthlyPrincipal = monthlyPrincipal;
//        this.monthlyInterest = monthlyInterest;
//        this.prepayPrincipal = prepayPrincipal;
//        this.actualRepay = actualRepay;
//        this.remainingPrincipal = remainingPrincipal;
//        this.isPrepay = isPrepay;
//    }
//
//    // Getter & Setter
//    public int getMonth() { return month; }
//    public BigDecimal getMonthlyPrincipal() { return monthlyPrincipal; }
//    public BigDecimal getMonthlyInterest() { return monthlyInterest; }
//    public BigDecimal getPrepayPrincipal() { return prepayPrincipal; }
//    public BigDecimal getActualRepay() { return actualRepay; }
//    public BigDecimal getRemainingPrincipal() { return remainingPrincipal; }
//    public boolean isPrepay() { return isPrepay; }
//
//    // 重写toString，方便打印测试
//    @Override
//    public String toString() {
//        if (isPrepay) {
//            return "第" + month + "月【提前还款】 | 正常本金：" + monthlyPrincipal + "元 | 当月利息：" + monthlyInterest +
//                    "元 | 提前还本金：" + prepayPrincipal + "元 | 当月总还款：" + actualRepay +
//                    "元 | 剩余本金：" + remainingPrincipal + "元";
//        } else {
//            return "第" + month + "月【正常还款】 | 当月本金：" + monthlyPrincipal + "元 | 当月利息：" + monthlyInterest +
//                    "元 | 当月还款：" + actualRepay + "元 | 剩余本金：" + remainingPrincipal + "元";
//        }
//    }
//}
//
///**
// * 提前还款参数实体类
// * 存储：提前还款的月份、提前还款的本金金额
// */
//class PrepayParam {
//    // 提前还款的月份（如第12个月还款）
//    private int prepayMonth;
//    // 提前还款的本金金额（元）
//    private BigDecimal prepayPrincipal;
//
//    public PrepayParam(int prepayMonth, BigDecimal prepayPrincipal) {
//        this.prepayMonth = prepayMonth;
//        this.prepayPrincipal = prepayPrincipal;
//    }
//
//    // Getter
//    public int getPrepayMonth() { return prepayMonth; }
//    public BigDecimal getPrepayPrincipal() { return prepayPrincipal; }
//}
//
///**
// * 等额本息计算工具类（支持提前还款）
// * 核心：原生Java + BigDecimal高精度 + 单次/多次提前还款
// */
//public class EqualInterestWithPrepayCalculator {
//    // 金融计算固定配置：保留2位小数，四舍五入
//    private static final int SCALE = 2;
//    private static final RoundingMode ROUND_MODE = RoundingMode.HALF_UP;
//    // 月利率计算保留8位小数，保证中间步骤精度
//    private static final int RATE_SCALE = 8;
//
//    /**
//     * 等额本息核心计算（支持提前还款）
//     * @param loanTotal 原始贷款总额（元，如1000000）
//     * @param annualRate 年利率（%，如4.25）
//     * @param loanYears 原始贷款年限（年，如30）
//     * @param prepayParams 提前还款参数列表（可传多个，按还款月份排序）
//     * @return 完整还款明细列表（含提前还款节点）
//     */
//    public static List<EqualInterestDetail2> calculate(BigDecimal loanTotal, BigDecimal annualRate,
//                                                       int loanYears, List<PrepayParam> prepayParams) {
//        List<EqualInterestDetail2> detailList = new ArrayList<>();
//        // 1. 基础参数初始化
//        int totalMonths = loanYears * 12; // 原始总还款月数
//        BigDecimal monthRate = annualRate.divide(new BigDecimal("12"), RATE_SCALE, ROUND_MODE)
//                .divide(new BigDecimal("100"), RATE_SCALE, ROUND_MODE); // 月利率（高精度）
//        BigDecimal remainingPrincipal = loanTotal; // 初始剩余本金
//        BigDecimal currentMonthlyRepay = calculateMonthlyRepay(remainingPrincipal, monthRate, totalMonths); // 初始月供
//
//        // 2. 遍历每个还款月，逐月累加计算
//        for (int month = 1; month <= totalMonths; month++) {
//            // 终止条件：剩余本金为0，无需继续还款
//            if (remainingPrincipal.compareTo(BigDecimal.ZERO) == 0) {
//                break;
//            }
//
//            // 检查当前月份是否为提前还款月
//            PrepayParam currentPrepay = getPrepayParamByMonth(prepayParams, month);
//            if (currentPrepay != null) {
//                // ========== 提前还款月份计算 ==========
//                BigDecimal prepayPrincipal = currentPrepay.getPrepayPrincipal();
//                // 校验：提前还款本金不能大于剩余本金（至少留0.01元，避免除零）
//                if (prepayPrincipal.compareTo(remainingPrincipal) >= 0) {
//                    prepayPrincipal = remainingPrincipal.subtract(new BigDecimal("0.01"));
//                }
//                // 当月利息：按还款前剩余本金计算（正常计收）
//                BigDecimal monthlyInterest = remainingPrincipal.multiply(monthRate).setScale(SCALE, ROUND_MODE);
//                // 正常月供的本金部分（无提前还款时的当月本金）
//                BigDecimal normalPrincipal = currentMonthlyRepay.subtract(monthlyInterest);
//                // 提前还款后当月剩余本金 = 原剩余本金 - 提前还款本金
//                BigDecimal newRemaining = remainingPrincipal.subtract(prepayPrincipal).setScale(SCALE, ROUND_MODE);
//                // 当月实际总还款 = 当月利息 + 提前还款本金（正常本金不再单独支付，直接抵扣）
//                BigDecimal actualRepay = monthlyInterest.add(prepayPrincipal).setScale(SCALE, ROUND_MODE);
//                // 添加提前还款明细
//                detailList.add(new EqualInterestDetail2(month, normalPrincipal, monthlyInterest,
//                        prepayPrincipal, actualRepay, newRemaining, true));
//                // 更新剩余本金+重新计算后续月供
//                remainingPrincipal = newRemaining;
//                int remainingMonths = totalMonths - month; // 剩余还款月份
//                currentMonthlyRepay = calculateMonthlyRepay(remainingPrincipal, monthRate, remainingMonths);
//            } else {
//                // ========== 正常还款月份计算 ==========
//                BigDecimal monthlyInterest;
//                BigDecimal monthlyPrincipal;
//                BigDecimal actualRepay;
//                BigDecimal newRemaining;
//
//                if (month == totalMonths || remainingPrincipal.compareTo(currentMonthlyRepay) < 0) {
//                    // 最后一月/剩余本金不足月供：抹平精度，剩余本金全额为本金，利息正常计算
//                    monthlyInterest = remainingPrincipal.multiply(monthRate).setScale(SCALE, ROUND_MODE);
//                    monthlyPrincipal = remainingPrincipal;
//                    actualRepay = monthlyPrincipal.add(monthlyInterest).setScale(SCALE, ROUND_MODE);
//                    newRemaining = BigDecimal.ZERO;
//                } else {
//                    // 正常月份：利息=剩余本金×月利率，本金=月供-利息
//                    monthlyInterest = remainingPrincipal.multiply(monthRate).setScale(SCALE, ROUND_MODE);
//                    monthlyPrincipal = currentMonthlyRepay.subtract(monthlyInterest);
//                    actualRepay = currentMonthlyRepay;
//                    newRemaining = remainingPrincipal.subtract(monthlyPrincipal).setScale(SCALE, ROUND_MODE);
//                }
//                // 添加正常还款明细
//                detailList.add(new EqualInterestDetail2(month, monthlyPrincipal, monthlyInterest,
//                        actualRepay, newRemaining));
//                // 更新剩余本金
//                remainingPrincipal = newRemaining;
//            }
//        }
//        return detailList;
//    }
//
//    /**
//     * 辅助方法：计算单期等额本息月供
//     * @param principal 剩余本金
//     * @param monthRate 月利率
//     * @param remainingMonths 剩余还款月份
//     * @return 月供金额（保留2位小数）
//     */
//    private static BigDecimal calculateMonthlyRepay(BigDecimal principal, BigDecimal monthRate, int remainingMonths) {
//        if (remainingMonths <= 0 || principal.compareTo(BigDecimal.ZERO) <= 0) {
//            return BigDecimal.ZERO;
//        }
//        // 计算 (1+月利率)^剩余月份
//        BigDecimal powRate = BigDecimal.ONE.add(monthRate).pow(remainingMonths);
//        // 分子：本金 × 月利率 × (1+月利率)^剩余月份
//        BigDecimal numerator = principal.multiply(monthRate).multiply(powRate);
//        // 分母：(1+月利率)^剩余月份 - 1
//        BigDecimal denominator = powRate.subtract(BigDecimal.ONE);
//        // 月供（保留2位小数）
//        return numerator.divide(denominator, SCALE, ROUND_MODE);
//    }
//
//    /**
//     * 辅助方法：根据月份获取提前还款参数
//     * @param prepayParams 提前还款列表
//     * @param month 当期月份
//     * @return 当期提前还款参数（无则返回null）
//     */
//    private static PrepayParam getPrepayParamByMonth(List<PrepayParam> prepayParams, int month) {
//        if (prepayParams == null || prepayParams.isEmpty()) {
//            return null;
//        }
//        for (PrepayParam param : prepayParams) {
//            if (param.getPrepayMonth() == month) {
//                return param;
//            }
//        }
//        return null;
//    }
//
//    /**
//     * 计算还款汇总信息
//     * @param loanTotal 原始贷款总额
//     * @param detailList 完整还款明细
//     * @return 汇总数组：[总还款月数, 总支付利息, 总实际还款额, 提前还款总本金]
//     */
//    public static BigDecimal[] calculateSummary(BigDecimal loanTotal, List<EqualInterestDetail2> detailList) {
//        int totalMonths = detailList.size();
//        BigDecimal totalInterest = BigDecimal.ZERO; // 总支付利息
//        BigDecimal totalActualRepay = BigDecimal.ZERO; // 总实际还款额
//        BigDecimal totalPrepayPrincipal = BigDecimal.ZERO; // 提前还款总本金
//
//        for (EqualInterestDetail2 detail : detailList) {
//            totalInterest = totalInterest.add(detail.getMonthlyInterest());
//            totalActualRepay = totalActualRepay.add(detail.getActualRepay());
//            totalPrepayPrincipal = totalPrepayPrincipal.add(detail.getPrepayPrincipal());
//        }
//        // 保留2位小数
//        totalInterest = totalInterest.setScale(SCALE, ROUND_MODE);
//        totalActualRepay = totalActualRepay.setScale(SCALE, ROUND_MODE);
//        totalPrepayPrincipal = totalPrepayPrincipal.setScale(SCALE, ROUND_MODE);
//
//        return new BigDecimal[]{
//                new BigDecimal(totalMonths), // 0:总还款月数
//                totalInterest,               // 1:总支付利息
//                totalActualRepay,            // 2:总实际还款额
//                totalPrepayPrincipal         // 3:提前还款总本金
//        };
//    }
//
//    // 测试主方法：直接运行验证提前还款计算结果
//    public static void main(String[] args) {
//        // 基础贷款参数：100万，年利率4.25%，贷款30年（360个月）
//        BigDecimal loanTotal = new BigDecimal("1000000");
//        BigDecimal annualRate = new BigDecimal("4.25");
//        int loanYears = 30;
//
//        // 提前还款参数：第12个月提前还款20万元（支持添加多个，如new PrepayParam(60, new BigDecimal("300000"))）
//        List<PrepayParam> prepayParams = new ArrayList<>();
//        prepayParams.add(new PrepayParam(12, new BigDecimal("200000")));
//
//        // 执行计算（含提前还款）
//        List<EqualInterestDetail2> detailList = calculate(loanTotal, annualRate, loanYears, prepayParams);
//        BigDecimal[] summary = calculateSummary(loanTotal, detailList);
//
//        // 打印汇总信息
//        System.out.println("======= 等额本息（含提前还款）还款汇总 =======");
//        System.out.println("原始贷款总额：" + loanTotal.setScale(SCALE) + " 元");
//        System.out.println("贷款年利率：" + annualRate + " %");
//        System.out.println("原始贷款年限：" + loanYears + " 年（" + loanYears*12 + " 个月）");
//        System.out.println("实际还款总月数：" + summary[0].intValue() + " 个月");
//        System.out.println("提前还款总本金：" + summary[3] + " 元");
//        System.out.println("总支付利息：" + summary[1] + " 元");
//        System.out.println("总实际还款额（本金+利息）：" + summary[2] + " 元");
//        System.out.println("节省利息：" + calculateSaveInterest(loanTotal, annualRate, loanYears, summary[1]) + " 元");
//        System.out.println("===========================================\n");
//
//        // 打印关键节点明细：前15个月（含第12个月提前还款）+ 最后1个月
//        System.out.println("======= 前15个月还款明细（含提前还款节点） =======");
//        for (int i = 0; i < Math.min(15, detailList.size()); i++) {
//            System.out.println(detailList.get(i));
//        }
//        System.out.println("\n======= 最后1个月还款明细 =======");
//        System.out.println(detailList.get(detailList.size() - 1));
//    }
//
//    /**
//     * 辅助方法：计算提前还款后节省的利息（对比无提前还款的总利息）
//     */
//    private static BigDecimal calculateSaveInterest(BigDecimal loanTotal, BigDecimal annualRate,
//                                                    int loanYears, BigDecimal actualTotalInterest) {
//        // 无提前还款的总利息
//        int totalMonths = loanYears * 12;
//        BigDecimal monthRate = annualRate.divide(new BigDecimal("12"), RATE_SCALE, ROUND_MODE)
//                .divide(new BigDecimal("100"), RATE_SCALE, ROUND_MODE);
//        BigDecimal originalMonthlyRepay = calculateMonthlyRepay(loanTotal, monthRate, totalMonths);
//        BigDecimal originalTotalRepay = originalMonthlyRepay.multiply(new BigDecimal(totalMonths - 1))
//                .add(calculateLastMonthRepay(loanTotal, monthRate, totalMonths));
//        BigDecimal originalTotalInterest = originalTotalRepay.subtract(loanTotal).setScale(SCALE, ROUND_MODE);
//        // 节省的利息 = 原总利息 - 实际总利息
//        return originalTotalInterest.subtract(actualTotalInterest).setScale(SCALE, ROUND_MODE);
//    }
//
//    /**
//     * 辅助方法：计算无提前还款时最后一月的还款额
//     */
//    private static BigDecimal calculateLastMonthRepay(BigDecimal loanTotal, BigDecimal monthRate, int totalMonths) {
//        BigDecimal remaining = loanTotal;
//        BigDecimal monthlyRepay = calculateMonthlyRepay(loanTotal, monthRate, totalMonths);
//        for (int i = 1; i < totalMonths; i++) {
//            BigDecimal interest = remaining.multiply(monthRate).setScale(SCALE, ROUND_MODE);
//            BigDecimal principal = monthlyRepay.subtract(interest);
//            remaining = remaining.subtract(principal).setScale(SCALE, ROUND_MODE);
//        }
//        BigDecimal lastInterest = remaining.multiply(monthRate).setScale(SCALE, ROUND_MODE);
//        return remaining.add(lastInterest);
//    }
//}