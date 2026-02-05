//package com.repay.controller;
//
//import com.repay.entity.*;
//import com.repay.service.RepayCalculator;
//import com.repay.constant.CONSTANT;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import jakarta.validation.Valid;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.math.BigDecimal;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
///**
// * 还款计算 Controller（支持组合贷）
// */
//@RestController
//@RequestMapping("/api/repay")
//@Tag(name = "还款计算接口", description = "等额本金还款计算相关接口（支持组合贷）")
//public class RepayController {
//    @Qualifier(value = "EqualPrincipalParamValidator")
//    @Autowired
//    private RepayCalculator repayCalculator;
//
//    /**
//     * 组合贷款（商贷+公积金）等额本金计算接口
//     */
//    @PostMapping("/combination-principal")
//    @Operation(summary = "组合贷等额本金还款计算", description = "支持纯商贷、纯公积金贷、组合贷的还款计算")
//    public CombinationLoanResponse calculateCombinationPrincipal(
//            @Valid @RequestBody CombinationLoanRequest request) {
//        CombinationLoanResponse response = new CombinationLoanResponse();
//
//        // 1. 分别计算商贷和公积金贷明细
//        EqualPrincipalRepayResponse businessResponse = null;
//        EqualPrincipalRepayResponse fundResponse = null;
//
//        // 纯商贷/组合贷：计算商贷明细
//        if ("single".equals(request.getLoanType()) || "combination".equals(request.getLoanType())) {
//            EqualPrincipalRepayRequest businessReq = buildSingleLoanRequest(
//                    request.getBusinessLoanTotal(),
//                    request.getBusinessAnnualRate(),
//                    request.getBusinessYears(),
//                    request.getReservedPrincipal(),
//                    request.getPeriod(),
//                    request.getPrepayments(),
//                    request.getPeriodicRepayList()
//            );
//            businessResponse = calculateSingleLoan(businessReq);
//            // 设置商贷明细
//            response.setBusinessMonthlyDetails(businessResponse.getMonthlyDetails());
//            response.setBusinessYearSummaries(businessResponse.getYearSummaries());
//            response.setBusinessTotalPrincipal(businessResponse.getTotalAllPrincipal());
//            response.setBusinessTotalInterest(businessResponse.getTotalAllInterest());
//            response.setBusinessTotalRepay(businessResponse.getTotalAllRepay());
//        }
//
//        // 纯公积金/组合贷：计算公积金贷明细
//        if ("fund".equals(request.getLoanType()) || "combination".equals(request.getLoanType())) {
//            EqualPrincipalRepayRequest fundReq = buildSingleLoanRequest(
//                    request.getFundLoanTotal(),
//                    request.getFundAnnualRate(),
//                    request.getFundYears(),
//                    request.getReservedPrincipal(),
//                    request.getPeriod(),
//                    request.getPrepayments(),
//                    request.getPeriodicRepayList()
//            );
//            fundResponse = calculateSingleLoan(fundReq);
//            // 设置公积金明细
//            response.setFundMonthlyDetails(fundResponse.getMonthlyDetails());
//            response.setFundYearSummaries(fundResponse.getYearSummaries());
//            response.setFundTotalPrincipal(fundResponse.getTotalAllPrincipal());
//            response.setFundTotalInterest(fundResponse.getTotalAllInterest());
//            response.setFundTotalRepay(fundResponse.getTotalAllRepay());
//        }
//
//        // 2. 合并总计（商+公）
//        BigDecimal totalPrincipal = BigDecimal.ZERO;
//        BigDecimal totalInterest = BigDecimal.ZERO;
//        BigDecimal totalRepay = BigDecimal.ZERO;
//
//        if (businessResponse != null) {
//            totalPrincipal = totalPrincipal.add(businessResponse.getTotalAllPrincipal());
//            totalInterest = totalInterest.add(businessResponse.getTotalAllInterest());
//            totalRepay = totalRepay.add(businessResponse.getTotalAllRepay());
//        }
//        if (fundResponse != null) {
//            totalPrincipal = totalPrincipal.add(fundResponse.getTotalAllPrincipal());
//            totalInterest = totalInterest.add(fundResponse.getTotalAllInterest());
//            totalRepay = totalRepay.add(fundResponse.getTotalAllRepay());
//        }
//
//        response.setTotalAllPrincipal(totalPrincipal);
//        response.setTotalAllInterest(totalInterest);
//        response.setTotalAllRepay(totalRepay);
//
//        // 3. 生成合并后的月度明细（单月总还款=商贷+公积金）
//        List<EqualPrincipalRepayResponse.MonthlyDetail> mergeMonthlyDetails = new ArrayList<>();
//        int maxMonths = 0;
//        if (businessResponse != null) maxMonths = Math.max(maxMonths, businessResponse.getTotalMonths());
//        if (fundResponse != null) maxMonths = Math.max(maxMonths, fundResponse.getTotalMonths());
//
//        for (int month = 1; month <= maxMonths; month++) {
//            EqualPrincipalRepayResponse.MonthlyDetail mergeDetail = new EqualPrincipalRepayResponse.MonthlyDetail();
//            mergeDetail.setMonth(month);
//
//            // 商贷当月明细
//            BigDecimal businessPrincipal = BigDecimal.ZERO;
//            BigDecimal businessInterest = BigDecimal.ZERO;
//            if (businessResponse != null && month <= businessResponse.getMonthlyDetails().size()) {
//                EqualPrincipalRepayResponse.MonthlyDetail bDetail = businessResponse.getMonthlyDetails().get(month - 1);
//                businessPrincipal = bDetail.getMonthlyPrincipal();
//                businessInterest = bDetail.getMonthlyInterest();
//            }
//
//            // 公积金当月明细
//            BigDecimal fundPrincipal = BigDecimal.ZERO;
//            BigDecimal fundInterest = BigDecimal.ZERO;
//            if (fundResponse != null && month <= fundResponse.getMonthlyDetails().size()) {
//                EqualPrincipalRepayResponse.MonthlyDetail fDetail = fundResponse.getMonthlyDetails().get(month - 1);
//                fundPrincipal = fDetail.getMonthlyPrincipal();
//                fundInterest = fDetail.getMonthlyInterest();
//            }
//
//            // 合并当月数据
//            mergeDetail.setMonthlyPrincipal(businessPrincipal.add(fundPrincipal));
//            mergeDetail.setMonthlyInterest(businessInterest.add(fundInterest));
//            mergeDetail.setMonthlyRepay(mergeDetail.getMonthlyPrincipal().add(mergeDetail.getMonthlyInterest()));
//
//            // 剩余本金（取最大的剩余本金）
//            BigDecimal remainingPrincipal = BigDecimal.ZERO;
//            if (businessResponse != null && month <= businessResponse.getMonthlyDetails().size()) {
//                remainingPrincipal = businessResponse.getMonthlyDetails().get(month - 1).getRemainingPrincipal();
//            }
//            if (fundResponse != null && month <= fundResponse.getMonthlyDetails().size()) {
//                BigDecimal fRemaining = fundResponse.getMonthlyDetails().get(month - 1).getRemainingPrincipal();
//                remainingPrincipal = remainingPrincipal.add(fRemaining);
//            }
//            mergeDetail.setRemainingPrincipal(remainingPrincipal);
//
//            mergeMonthlyDetails.add(mergeDetail);
//        }
//
//        response.setMonthlyDetails(mergeMonthlyDetails);
//        response.setTotalMonths(maxMonths);
//
//        return response;
//    }
//
//    /**
//     * 构建单笔贷款请求参数
//     */
//    private EqualPrincipalRepayRequest buildSingleLoanRequest(
//            BigDecimal loanTotal,
//            BigDecimal annualRate,
//            Integer years,
//            BigDecimal reservedPrincipal,
//            Integer period,
//            List<Prepayment> prepayments,
//            List<PeriodRepay> periodicRepayList) {
//        EqualPrincipalRepayRequest req = new EqualPrincipalRepayRequest();
//        req.setLoanTotal(loanTotal);
//        req.setAnnualRate(annualRate);
//        req.setYears(years);
//        req.setReservedPrincipal(reservedPrincipal);
//        req.setPeriod(period);
//        req.setPrepayments(prepayments);
//        req.setPeriodicRepayList(periodicRepayList);
//        return req;
//    }
//
//    /**
//     * 计算单笔贷款（商贷/公积金）的还款明细（原有逻辑）
//     */
//    private EqualPrincipalRepayResponse calculateSingleLoan(EqualPrincipalRepayRequest request) {
//        BigDecimal annualRate = request.getAnnualRate();
//        BigDecimal loanTotal = request.getLoanTotal().setScale(CONSTANT.SCALE, CONSTANT.ROUND_MODE);
//        Integer years = request.getYears();
//        int totalMonths = years * 12;
//        BigDecimal monthlyPrincipal = loanTotal.divide(new BigDecimal(totalMonths), CONSTANT.SCALE, CONSTANT.ROUND_MODE);
//        BigDecimal remainingPrincipal = loanTotal;
//
//        Map<Integer, BigDecimal> prepayMoney = new HashMap<>();
//        if (!request.getPrepayments().isEmpty()) {
//            prepayMoney = repayCalculator.getAllPrepayMoney(request.getPrepayments());
//        }
//        prepayMoney = updatePayMoney(prepayMoney, request.getPeriodicRepayList());
//
//        List<EqualPrincipalRepayResponse.MonthlyDetail> monthlyDetails = new ArrayList<>();
//        List<EqualPrincipalRepayResponse.YearSummary> yearSummaries = new ArrayList<>();
//
//        BigDecimal totalAllPrincipal = BigDecimal.ZERO;
//        BigDecimal totalAllInterest = BigDecimal.ZERO;
//        BigDecimal yearPrincipal = BigDecimal.ZERO;
//        BigDecimal yearInterest = BigDecimal.ZERO;
//
//        Integer period = request.getPeriod();
//
//        for (int month = 1; month <= totalMonths; month++) {
//            BigDecimal monthlyInterest = repayCalculator.getMonthlyInterest(remainingPrincipal, annualRate);
//            remainingPrincipal = remainingPrincipal.subtract(monthlyPrincipal).setScale(CONSTANT.SCALE, CONSTANT.ROUND_MODE);
//            if (remainingPrincipal.compareTo(BigDecimal.ZERO) < 0) {
//                remainingPrincipal = BigDecimal.ZERO;
//            }
//
//            BigDecimal prepayMoneyCurrentMonth = BigDecimal.ZERO;
//            BigDecimal monthTotalPrincipal = monthlyPrincipal;
//            if (prepayMoney.containsKey(month)) {
//                prepayMoneyCurrentMonth = prepayMoney.get(month);
//                if (remainingPrincipal.subtract(prepayMoneyCurrentMonth).compareTo(request.getReservedPrincipal()) >= 0) {
//                    remainingPrincipal = remainingPrincipal.subtract(prepayMoney.get(month));
//                    monthTotalPrincipal = monthlyPrincipal.add(prepayMoneyCurrentMonth);
//                    if (totalMonths - month > 0) {
//                        monthlyPrincipal = remainingPrincipal.divide(
//                                new BigDecimal(totalMonths).subtract(new BigDecimal(month)),
//                                CONSTANT.SCALE, CONSTANT.ROUND_MODE
//                        );
//                    }
//                }
//            }
//
//            monthlyDetails.add(setMonthDetail(month, monthTotalPrincipal, monthlyInterest, remainingPrincipal));
//            yearPrincipal = yearPrincipal.add(monthTotalPrincipal);
//            yearInterest = yearInterest.add(monthlyInterest);
//            totalAllPrincipal = totalAllPrincipal.add(monthTotalPrincipal);
//            totalAllInterest = totalAllInterest.add(monthlyInterest);
//
//            if (month % 12 == 0) {
//                int currentYear = month / 12;
//                yearSummaries.add(setYearDetail(currentYear, yearPrincipal, yearInterest));
//                yearPrincipal = BigDecimal.ZERO;
//                yearInterest = BigDecimal.ZERO;
//            }
//        }
//
//        BigDecimal totalAllRepay = totalAllPrincipal.add(totalAllInterest);
//        return new EqualPrincipalRepayResponse()
//                .setMonthlyDetails(monthlyDetails)
//                .setYearSummaries(yearSummaries)
//                .setTotalAllPrincipal(totalAllPrincipal)
//                .setTotalAllInterest(totalAllInterest)
//                .setTotalAllRepay(totalAllRepay)
//                .setLoanTotal(loanTotal)
//                .setAnnualRate(annualRate)
//                .setYears(years)
//                .setTotalMonths(totalMonths);
//    }
//
//    /**
//     * 封装月度明细（复用原有逻辑）
//     */
//    private EqualPrincipalRepayResponse.MonthlyDetail setMonthDetail(int month,
//                                                                     BigDecimal monthTotalPrincipal,
//                                                                     BigDecimal monthlyInterest,
//                                                                     BigDecimal remainingPrincipal) {
//        EqualPrincipalRepayResponse.MonthlyDetail monthlyDetail = new EqualPrincipalRepayResponse.MonthlyDetail();
//        monthlyDetail.setMonth(month);
//        monthlyDetail.setMonthlyPrincipal(monthTotalPrincipal);
//        monthlyDetail.setMonthlyInterest(monthlyInterest);
//        monthlyDetail.setMonthlyRepay(monthlyInterest.add(monthTotalPrincipal));
//        monthlyDetail.setRemainingPrincipal(remainingPrincipal);
//        return monthlyDetail;
//    }
//
//    /**
//     * 封装年度明细（复用原有逻辑）
//     */
//    private EqualPrincipalRepayResponse.YearSummary setYearDetail(int currentYear, BigDecimal yearPrincipal,
//                                                                  BigDecimal yearInterest) {
//        EqualPrincipalRepayResponse.YearSummary yearSummary = new EqualPrincipalRepayResponse.YearSummary();
//        yearSummary.setYear(currentYear);
//        yearSummary.setYearPrincipal(yearPrincipal);
//        yearSummary.setYearInterest(yearInterest);
//        yearSummary.setYearTotalRepay(yearInterest.add(yearPrincipal));
//        return yearSummary;
//    }
//
//    /**
//     * 更新周期还款金额（复用原有逻辑）
//     */
//    private Map<Integer, BigDecimal> updatePayMoney(Map<Integer, BigDecimal> prepayMoney, List<PeriodRepay> periodRepayList) {
//        Map<Integer, BigDecimal> resultMap = prepayMoney == null ? new HashMap<>() : prepayMoney;
//        if (periodRepayList == null || periodRepayList.isEmpty()) {
//            return resultMap;
//        }
//
//        for (PeriodRepay periodRepay : periodRepayList) {
//            if (periodRepay == null || periodRepay.getStartMonth() == null || periodRepay.getEndMonth() == null ||
//                    periodRepay.getCycleMonths() == null || periodRepay.getAmount() == null) {
//                continue;
//            }
//
//            int startMonth = periodRepay.getStartMonth();
//            int endMonth = periodRepay.getEndMonth();
//            int cycleMonths = periodRepay.getCycleMonths();
//            BigDecimal amount = periodRepay.getAmount();
//
//            if (cycleMonths <= 0 || startMonth > endMonth) {
//                continue;
//            }
//
//            for (int i = startMonth; i <= endMonth; i += cycleMonths) {
//                BigDecimal oldAmount = resultMap.getOrDefault(i, BigDecimal.ZERO);
//                resultMap.put(i, oldAmount.add(amount));
//            }
//        }
//
//        return resultMap;
//    }
//
//    // 保留原有等额本金接口（兼容旧调用）
//    @PostMapping("/equal-principal")
//    @Operation(summary = "等额本金还款计算（单笔贷）", description = "原有单笔贷款计算接口，兼容旧调用")
//    public EqualPrincipalRepayResponse calculateEqualPrincipal(@Valid @RequestBody EqualPrincipalRepayRequest request) {
//        return calculateSingleLoan(request);
//    }
//}