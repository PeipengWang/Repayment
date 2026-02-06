package com.repay.service.impl;

import com.repay.entity.*;
import com.repay.service.RepayCalculator;
import com.repay.constant.CONSTANT;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.repay.constant.CONSTANT.ROUND_MODE;
import static com.repay.constant.CONSTANT.SCALE;

/**
 * 等额本金计算实现类（含提前还款逻辑）
 */
@Service("EqualPrincipalParamValidator") // 匹配Controller中的@Qualifier名称
public class EqualPrincipalCalculatorImpl implements RepayCalculator {

    /**
     * 计算当月利息：剩余本金 × 月利率（年利率/1200）
     */
    @Override
    public BigDecimal getMonthlyInterest(BigDecimal remainingPrincipal, BigDecimal annualRate) {
        // 月利率 = 年利率 / 12 / 100 = 年利率 / 1200
        BigDecimal monthlyRate = annualRate.divide(new BigDecimal(1200), 6, CONSTANT.ROUND_MODE);
        return remainingPrincipal.multiply(monthlyRate).setScale(CONSTANT.SCALE, CONSTANT.ROUND_MODE);
    }

    /**
     * 转换提前还款列表为Map（便于快速查询）
     */
    @Override
    public Map<Integer, BigDecimal> getAllPrepayMoney(List<Prepayment> prepayments) {
        Map<Integer, BigDecimal> prepayMap = new HashMap<>();
        if (prepayments == null || prepayments.isEmpty()) {
            return prepayMap;
        }
        for (Prepayment prepayment : prepayments) {
            // 提前还款金额保留2位小数
            BigDecimal amount = prepayment.getAmount().setScale(CONSTANT.SCALE, CONSTANT.ROUND_MODE);
            prepayMap.put(prepayment.getMonth(), amount);
        }
        return prepayMap;
    }

    @Override
    public BigDecimal getMonthlyPrincipal(int totalMonths, int nowMonth, BigDecimal remainingPrincipal) {
       return remainingPrincipal.divide(new BigDecimal(totalMonths - nowMonth), CONSTANT.SCALE, CONSTANT.ROUND_MODE);
    }

    @Override
    public CombinationLoanResponse getTotalResponse(EqualPrincipalRepayResponse businessResponse,
                                                     EqualPrincipalRepayResponse fundResponse,
                                                     CombinationLoanResponse  response){
        // 2. 合并总计（商+公）
        BigDecimal totalPrincipal = BigDecimal.ZERO;
        BigDecimal totalInterest = BigDecimal.ZERO;
        BigDecimal totalRepay = BigDecimal.ZERO;
        if (businessResponse != null) {
            totalPrincipal = totalPrincipal.add(businessResponse.getTotalAllPrincipal());
            totalInterest = totalInterest.add(businessResponse.getTotalAllInterest());
            totalRepay = totalRepay.add(businessResponse.getTotalAllRepay());
        }
        if (fundResponse != null) {
            totalPrincipal = totalPrincipal.add(fundResponse.getTotalAllPrincipal());
            totalInterest = totalInterest.add(fundResponse.getTotalAllInterest());
            totalRepay = totalRepay.add(fundResponse.getTotalAllRepay());
        }

        response.setTotalAllPrincipal(totalPrincipal);
        response.setTotalAllInterest(totalInterest);
        response.setTotalAllRepay(totalRepay);

        // 3. 生成合并后的月度明细（单月总还款=商贷+公积金）
        List<EqualPrincipalRepayResponse.MonthlyDetail> mergeMonthlyDetails = new ArrayList<>();
        int maxMonths = 0;
        if (businessResponse != null) maxMonths = Math.max(maxMonths, businessResponse.getTotalMonths());
        if (fundResponse != null) maxMonths = Math.max(maxMonths, fundResponse.getTotalMonths());

        for (int month = 1; month <= maxMonths; month++) {
            EqualPrincipalRepayResponse.MonthlyDetail mergeDetail = new EqualPrincipalRepayResponse.MonthlyDetail();
            mergeDetail.setMonth(month);
            // 商贷当月明细
            BigDecimal businessPrincipal = BigDecimal.ZERO;
            BigDecimal businessInterest = BigDecimal.ZERO;
            if (businessResponse != null && month <= businessResponse.getMonthlyDetails().size()) {
                EqualPrincipalRepayResponse.MonthlyDetail bDetail = businessResponse.getMonthlyDetails().get(month - 1);
                businessPrincipal = bDetail.getMonthlyPrincipal();
                businessInterest = bDetail.getMonthlyInterest();
            }

            // 公积金当月明细
            BigDecimal fundPrincipal = BigDecimal.ZERO;
            BigDecimal fundInterest = BigDecimal.ZERO;
            if (fundResponse != null && month <= fundResponse.getMonthlyDetails().size()) {
                EqualPrincipalRepayResponse.MonthlyDetail fDetail = fundResponse.getMonthlyDetails().get(month - 1);
                fundPrincipal = fDetail.getMonthlyPrincipal();
                fundInterest = fDetail.getMonthlyInterest();
            }

            // 合并当月数据
            mergeDetail.setMonthlyPrincipal(businessPrincipal.add(fundPrincipal));
            mergeDetail.setMonthlyInterest(businessInterest.add(fundInterest));
            mergeDetail.setMonthlyRepay(mergeDetail.getMonthlyPrincipal().add(mergeDetail.getMonthlyInterest()));

            // 剩余本金（取最大的剩余本金）
            BigDecimal remainingPrincipal = BigDecimal.ZERO;
            if (businessResponse != null && month <= businessResponse.getMonthlyDetails().size()) {
                remainingPrincipal = businessResponse.getMonthlyDetails().get(month - 1).getRemainingPrincipal();
            }
            if (fundResponse != null && month <= fundResponse.getMonthlyDetails().size()) {
                BigDecimal fRemaining = fundResponse.getMonthlyDetails().get(month - 1).getRemainingPrincipal();
                remainingPrincipal = remainingPrincipal.add(fRemaining);
            }
            mergeDetail.setRemainingPrincipal(remainingPrincipal);

            mergeMonthlyDetails.add(mergeDetail);
        }
        response.setMonthlyDetails(mergeMonthlyDetails);
        response.setTotalMonths(maxMonths);
        return response;
    }

    @Override
    public EqualPrincipalRepayResponse  businessResponse(CombinationLoanRequest request,
                                                           Map<Integer, BigDecimal> prepayMoney,
                                                           CombinationLoanResponse response,String type){
        EqualPrincipalRepayResponse businessResponse = null;
        // 纯商贷/组合贷：计算商贷明细
        if ("single".equals(request.getLoanType()) || "combination".equals(request.getLoanType())) {
            EqualPrincipalRepayRequest businessReq = buildSingleLoanRequest(
                    request.getBusinessLoanTotal(),
                    request.getBusinessAnnualRate(),
                    request.getBusinessYears(),
                    request.getReservedPrincipal()
            );
            if(type.equals("equalPrincipal")){
                businessResponse = calculatorPrincipal(businessReq, prepayMoney);
            }else {
                businessResponse = calculateEqualInterestRepay(businessReq, prepayMoney);
            }

            // 设置商贷明细
            response.setBusinessMonthlyDetails(businessResponse.getMonthlyDetails());
            response.setBusinessYearSummaries(businessResponse.getYearSummaries());
            response.setBusinessTotalPrincipal(businessResponse.getTotalAllPrincipal());
            response.setBusinessTotalInterest(businessResponse.getTotalAllInterest());
            response.setBusinessTotalRepay(businessResponse.getTotalAllRepay());
            response.setYears(businessReq.getYears());
        }
        return businessResponse;
    }


    @Override
    public EqualPrincipalRepayResponse  fundResponse(CombinationLoanRequest request,
                                                       Map<Integer, BigDecimal> prepayMoney,
                                                       CombinationLoanResponse response,String type ){
        EqualPrincipalRepayResponse fundResponse = null;

        // 纯公积金/组合贷：计算公积金贷明细
        if ("fund".equals(request.getLoanType()) || "combination".equals(request.getLoanType())) {
            EqualPrincipalRepayRequest fundReq = buildSingleLoanRequest(
                    request.getFundLoanTotal(),
                    request.getFundAnnualRate(),
                    request.getFundYears(),
                    request.getReservedPrincipal()
            );
            if(type.equals("equalPrincipal")){
                fundResponse = calculatorPrincipal(fundReq, prepayMoney);

            }else {
                fundResponse = calculateEqualInterestRepay(fundReq, prepayMoney);
            }
            // 设置公积金明细
            response.setFundMonthlyDetails(fundResponse.getMonthlyDetails());
            response.setFundYearSummaries(fundResponse.getYearSummaries());
            response.setFundTotalPrincipal(fundResponse.getTotalAllPrincipal());
            response.setFundTotalInterest(fundResponse.getTotalAllInterest());
            response.setFundTotalRepay(fundResponse.getTotalAllRepay());
            response.setYears(fundReq.getYears());
        }
        return fundResponse;
    }
    /**
     * 计算贷款汇总
     * @param request  贷款请求参数
     * @param prepayMoney 提前还款信息
     * @return 还款汇总
     */
    @Override
    public EqualPrincipalRepayResponse calculatorPrincipal(EqualPrincipalRepayRequest request, Map<Integer, BigDecimal> prepayMoney){
        int totalMonths = request.getYears() * 12;  //需要还款总月数
        //剩余本金
        BigDecimal remainingPrincipal = request.getLoanTotal();
        //获取所有提前还款年份

        // 4. 初始化统计变量和结果列表
        List<EqualPrincipalRepayResponse.MonthlyDetail> monthlyDetails = new ArrayList<>();
        List<EqualPrincipalRepayResponse.YearSummary> yearSummaries = new ArrayList<>();
        BigDecimal totalAllPrincipal = BigDecimal.ZERO; //已还总本金
        BigDecimal totalAllInterest = BigDecimal.ZERO; //已还总利息
        BigDecimal yearPrincipal = BigDecimal.ZERO; //年总本金
        BigDecimal yearInterest = BigDecimal.ZERO; //年总利息
        // 5. 遍历每月计算明细
        for (int month = 1; month <= totalMonths; month++) {
            //计算当月需要还款本金
            BigDecimal monthlyPrincipal = remainingPrincipal.divide(new BigDecimal(totalMonths).subtract(new BigDecimal(month-1)),
                    CONSTANT.SCALE, CONSTANT.ROUND_MODE);
            // 计算当月利息和还款额
            BigDecimal monthlyInterest = getMonthlyInterest(remainingPrincipal, request.getAnnualRate());
            // 更新剩余本金
            remainingPrincipal = remainingPrincipal.subtract(monthlyPrincipal).setScale(CONSTANT.SCALE, CONSTANT.ROUND_MODE);
            if (remainingPrincipal.compareTo(BigDecimal.ZERO) < 0) {
                remainingPrincipal = BigDecimal.ZERO;
            }
            BigDecimal prepayMoneyCurrentMonth = BigDecimal.ZERO;
            BigDecimal monthTotalPrincipal = monthlyPrincipal;
            if(prepayMoney.containsKey(month)){
                prepayMoneyCurrentMonth = prepayMoney.get(month);
                //检查提前还款是否还完剩余本金
                if(remainingPrincipal.subtract(prepayMoneyCurrentMonth).compareTo(request.getReservedPrincipal()) >=0){
                    //当前提前还款无法还完
                    remainingPrincipal = remainingPrincipal.subtract(prepayMoney.get(month));
                    monthTotalPrincipal = monthlyPrincipal.add(prepayMoneyCurrentMonth);
                    prepayMoney.remove(month);
                }else {
                    BigDecimal subtract1 = prepayMoneyCurrentMonth.subtract(remainingPrincipal);
                    prepayMoney.put(month, subtract1);
                    remainingPrincipal = BigDecimal.ZERO;
                }
            }
            // 封装当月明细
            monthlyDetails.add(setMonthDetail(month, monthTotalPrincipal, monthlyInterest, remainingPrincipal));
            // 累加统计数据
            yearPrincipal = yearPrincipal.add(monthTotalPrincipal);
            yearInterest = yearInterest.add(monthlyInterest);
            totalAllPrincipal = totalAllPrincipal.add(monthTotalPrincipal);
            totalAllInterest = totalAllInterest.add(monthlyInterest);

            // 每年结束时封装年度汇总
            if (month % 12 == 0) {
                int currentYear = month / 12;
                yearSummaries.add(setYearDetail(currentYear, yearPrincipal, yearInterest));
                // 重置当年统计变量
                yearPrincipal = BigDecimal.ZERO;
                yearInterest = BigDecimal.ZERO;
            }
        }
        // 6. 封装总计信息
        BigDecimal totalAllRepay = totalAllPrincipal.add(totalAllInterest);
        EqualPrincipalRepayResponse repayResponse = new EqualPrincipalRepayResponse();
        repayResponse.setMonthlyDetails(monthlyDetails);
        repayResponse.setYearSummaries(yearSummaries);
        repayResponse.setTotalAllPrincipal(totalAllPrincipal);
        repayResponse.setTotalAllInterest(totalAllInterest);
        repayResponse.setTotalAllRepay(totalAllRepay);
        repayResponse.setLoanTotal(request.getLoanTotal());
        repayResponse.setAnnualRate(request.getAnnualRate());
        repayResponse.setYears(request.getYears());
        repayResponse.setTotalMonths(totalMonths);
        return repayResponse;

    }

    /**
     * 构建单笔贷款请求参数
     */

    @Override
    public  EqualPrincipalRepayRequest buildSingleLoanRequest(
            BigDecimal loanTotal,
            BigDecimal annualRate,
            Integer years,
            BigDecimal reservedPrincipal) {
        EqualPrincipalRepayRequest req = new EqualPrincipalRepayRequest();
        req.setLoanTotal(loanTotal);
        req.setAnnualRate(annualRate);
        req.setYears(years);
        req.setReservedPrincipal(reservedPrincipal);

        return req;
    }

    /**
     *
     * @param month 当前月份
     * @param monthTotalPrincipal 月还款本金
     * @param monthlyInterest 月还款利息
     * @param remainingPrincipal 剩余未还款本金
     * @return 月度明细
     */
    @Override
    public EqualPrincipalRepayResponse.MonthlyDetail setMonthDetail(int month,
                                                                     BigDecimal monthTotalPrincipal,
                                                                     BigDecimal monthlyInterest,
                                                                     BigDecimal remainingPrincipal){
        EqualPrincipalRepayResponse.MonthlyDetail monthlyDetail = new EqualPrincipalRepayResponse.MonthlyDetail();
        monthlyDetail.setMonth(month);
        monthlyDetail.setMonthlyPrincipal(monthTotalPrincipal);
        monthlyDetail.setMonthlyInterest(monthlyInterest);
        monthlyDetail.setMonthlyRepay(monthlyInterest.add(monthTotalPrincipal));
        monthlyDetail.setRemainingPrincipal(remainingPrincipal);
        return monthlyDetail;
    }

    /**
     * 封装年度信息
     * @param currentYear 当前年份
     * @param yearPrincipal 年本金
     * @param yearInterest 年利息
     * @return  年度账单信息
     */
    @Override
    public  EqualPrincipalRepayResponse.YearSummary setYearDetail(int currentYear, BigDecimal yearPrincipal,
                                                                   BigDecimal yearInterest){
        EqualPrincipalRepayResponse.YearSummary yearSummary = new EqualPrincipalRepayResponse.YearSummary();
        yearSummary.setYear(currentYear);
        yearSummary.setYearPrincipal(yearPrincipal);
        yearSummary.setYearInterest(yearInterest);
        yearSummary.setYearTotalRepay(yearInterest.add(yearPrincipal));
        return yearSummary;
    }

    /**
     * 按周期更新各月份的应还款金额（保留原有金额并累加）
     * @param prepayMoney 原有月份-金额映射（可为空，内部做兜底）
     * @param periodRepayList 周期还款列表（可为空，空则直接返回原数据）
     * @return 更新后的月份-金额映射
     */
    @Override
    public Map<Integer, BigDecimal> updatePayMoney(Map<Integer, BigDecimal> prepayMoney, List<PeriodRepay> periodRepayList) {
        // 1. 兜底原Map，避免空指针，同时可选创建新Map避免修改入参（根据业务选择）
        // 如果需要保留原Map不变，用 new HashMap<>(prepayMoney == null ? new HashMap<>() : prepayMoney)
        Map<Integer, BigDecimal> resultMap = prepayMoney == null ? new HashMap<>() : prepayMoney;

        // 2. 空列表直接返回，避免NPE
        if (periodRepayList == null || periodRepayList.isEmpty()) {
            return resultMap;
        }

        // 3. 遍历每个还款周期
        for (PeriodRepay periodRepay : periodRepayList) {
            // 3.1 校验周期数据合法性，避免空指针/死循环
            if (periodRepay == null
                    || periodRepay.getStartMonth() == null
                    || periodRepay.getEndMonth() == null
                    || periodRepay.getCycleMonths() == null
                    || periodRepay.getAmount() == null) {
                continue; // 或抛异常，根据业务容错性选择
            }

            int startMonth = periodRepay.getStartMonth();
            int endMonth = periodRepay.getEndMonth();
            int cycleMonths = periodRepay.getCycleMonths();
            BigDecimal amount = periodRepay.getAmount();

            // 3.2 校验周期合法性：周期<=0 或 起始>结束，直接跳过
            if (cycleMonths <= 0 || startMonth > endMonth) {
                continue; // 或抛IllegalArgumentException
            }

            // 3.3 循环累加：注意用 <= 包含结束月份（根据业务调整，若不需要则改回 <）
            // 核心逻辑：保留原有值 + 新增金额
            for (int i = startMonth; i <= endMonth; i += cycleMonths) {
                BigDecimal oldAmount = resultMap.getOrDefault(i, BigDecimal.ZERO);
                resultMap.put(i, oldAmount.add(amount));
            }
        }

        return resultMap;
    }

    /**
     * 等额本息核心计算方法
     * @return 还款明细列表（包含每月数据，第一个元素为汇总信息，后续为每月明细）
     */
    @Override
    public EqualPrincipalRepayResponse calculateEqualInterestRepay(EqualPrincipalRepayRequest request, Map<Integer, BigDecimal> prepayMoney) {
        List<EqualPrincipalRepayResponse.MonthlyDetail> monthlyDetails = new ArrayList<>();
        List<EqualPrincipalRepayResponse.YearSummary> yearSummaries = new ArrayList<>();
        // 1. 基础参数计算（和等额本金对齐结构）
        Integer totalMonths = request.getYears() * 12; // 总还款月数
        BigDecimal monthRate = request.getAnnualRate().divide(new BigDecimal("12"), 8, ROUND_MODE)
                .divide(new BigDecimal("100"), 8, ROUND_MODE); // 月利率，保留8位小数保证精度
        BigDecimal remainingPrincipal = request.getLoanTotal(); // 初始剩余本金=贷款总额
        BigDecimal totalAllPrincipal = BigDecimal.ZERO; // 已还总本金
        BigDecimal totalAllInterest = BigDecimal.ZERO; // 已还总利息
        BigDecimal yearPrincipal = BigDecimal.ZERO; // 年总本金
        BigDecimal yearInterest = BigDecimal.ZERO; // 年总利息
        // 初始化每月固定月供（等额本息核心）
        BigDecimal monthlyFixedRepay = BigDecimal.ZERO;

        // 2. 首次计算初始固定月供（核心公式，补全pow方法精度）
        if (totalMonths > 0 && remainingPrincipal.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal powRate = BigDecimal.ONE.add(monthRate).pow(totalMonths);
            BigDecimal numerator = remainingPrincipal.multiply(monthRate).multiply(powRate);
            BigDecimal denominator = powRate.subtract(BigDecimal.ONE);
            monthlyFixedRepay = numerator.divide(denominator, SCALE, ROUND_MODE);
        }
        // 3. 循环计算每月明细（和等额本金遍历逻辑一致）
        for (int month = 1; month <= totalMonths; month++) {
            // 边界终止：剩余本金为0，无需继续计算
            if (remainingPrincipal.compareTo(BigDecimal.ZERO) == 0) {
                break;
            }
            BigDecimal monthlyInterest; // 当月利息
            BigDecimal monthlyPrincipal; // 当月正常还款本金
            BigDecimal monthTotalPrincipal = BigDecimal.ZERO; // 当月总本金（正常+提前还款）
            BigDecimal monthlyRepay = BigDecimal.ZERO; // 当月实际还款总额
            BigDecimal finalRemaining = remainingPrincipal; // 当月还款后剩余本金
            BigDecimal prepayMoneyCurrentMonth = BigDecimal.ZERO; // 当月提前还款本金

            // ========== 核心：处理当月提前还款 ==========
            if (prepayMoney.containsKey(month)) {
                prepayMoneyCurrentMonth = prepayMoney.get(month);
                if (remainingPrincipal.compareTo(prepayMoneyCurrentMonth) >= 0) {
                    // 情况1：提前还款 ≤ 剩余本金
                    finalRemaining = remainingPrincipal.subtract(prepayMoneyCurrentMonth).setScale(SCALE, ROUND_MODE);
                    monthTotalPrincipal = monthTotalPrincipal.add(prepayMoneyCurrentMonth);
                    prepayMoney.remove(month);
                } else {
                    // 情况2：提前还款 > 剩余本金 → 全部还清，多余部分忽略（不 put 回！）
                    monthTotalPrincipal = monthTotalPrincipal.add(remainingPrincipal);
                    finalRemaining = BigDecimal.ZERO;
                    // 注意：不再 put 回未使用部分，避免资金丢失或逻辑混乱
                    prepayMoney.remove(month); // 清除该月记录
                }

                // 重新计算月供（基于新的 finalRemaining 和剩余月份）
                int remainingMonths = totalMonths - month;
                if (remainingMonths > 0 && finalRemaining.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal powRate = BigDecimal.ONE.add(monthRate).pow(remainingMonths);
                    BigDecimal numerator = finalRemaining.multiply(monthRate).multiply(powRate);
                    BigDecimal denominator = powRate.subtract(BigDecimal.ONE);
                    monthlyFixedRepay = numerator.divide(denominator, SCALE, ROUND_MODE);
                } else {
                    monthlyFixedRepay = BigDecimal.ZERO;
                }
            }

            // ========== 计算当月利息（按月初剩余本金，即 remainingPrincipal）==========
            monthlyInterest = remainingPrincipal.multiply(monthRate).setScale(SCALE, ROUND_MODE);

            // ========== 计算正常还款本金 ==========
            if (month == totalMonths || finalRemaining.compareTo(request.getReservedPrincipal()) <= 0) {
                // 最后一期：还清至保留本金
                monthlyPrincipal = finalRemaining.subtract(request.getReservedPrincipal());
                if (monthlyPrincipal.compareTo(BigDecimal.ZERO) < 0) {
                    monthlyPrincipal = BigDecimal.ZERO;
                }
                monthlyRepay = monthlyPrincipal.add(monthlyInterest);
                monthTotalPrincipal = monthTotalPrincipal.add(monthlyPrincipal);
                finalRemaining = request.getReservedPrincipal();
            } else {
                // 正常月份
                monthlyPrincipal = monthlyFixedRepay.subtract(monthlyInterest);
                if (monthlyPrincipal.compareTo(BigDecimal.ZERO) < 0) {
                    monthlyPrincipal = BigDecimal.ZERO;
                }
                monthlyRepay = monthlyFixedRepay;
                monthTotalPrincipal = monthTotalPrincipal.add(monthlyPrincipal);
                // ✅ 关键修复：无论是否有提前还款，都要从 finalRemaining 中扣除正常还款本金
                finalRemaining = finalRemaining.subtract(monthlyPrincipal).setScale(SCALE, ROUND_MODE);
                // 防止因精度问题出现负数
                if (finalRemaining.compareTo(BigDecimal.ZERO) < 0) {
                    finalRemaining = BigDecimal.ZERO;
                }
            }

            // ========== 数据统计 ==========
            yearPrincipal = yearPrincipal.add(monthTotalPrincipal);
            yearInterest = yearInterest.add(monthlyInterest);
            totalAllPrincipal = totalAllPrincipal.add(monthTotalPrincipal);
            totalAllInterest = totalAllInterest.add(monthlyInterest);

            monthlyDetails.add(setMonthDetail(month, monthTotalPrincipal, monthlyInterest, finalRemaining));

            // ========== 年度汇总 ==========
            if (month % 12 == 0) {
                int currentYear = month / 12;
                yearSummaries.add(setYearDetail(currentYear, yearPrincipal, yearInterest));
                yearPrincipal = BigDecimal.ZERO;
                yearInterest = BigDecimal.ZERO;
            }

            // 更新剩余本金
            remainingPrincipal = finalRemaining;
        }

        // 4. 封装总计信息
        BigDecimal totalAllRepay = totalAllPrincipal.add(totalAllInterest);
        EqualPrincipalRepayResponse repayResponse = new EqualPrincipalRepayResponse();
        repayResponse.setMonthlyDetails(monthlyDetails);
        repayResponse.setYearSummaries(yearSummaries);
        repayResponse.setTotalAllPrincipal(totalAllPrincipal);
        repayResponse.setTotalAllInterest(totalAllInterest);
        repayResponse.setTotalAllRepay(totalAllRepay);
        repayResponse.setLoanTotal(request.getLoanTotal());
        repayResponse.setAnnualRate(request.getAnnualRate());
        repayResponse.setYears(request.getYears());
        repayResponse.setTotalMonths(totalMonths);
        return repayResponse;
    }
}