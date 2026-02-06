package com.repay.service;

import com.repay.entity.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 还款计算核心接口（支持提前还款）
 */
public interface RepayCalculator {

    /**
     * 计算当月利息
     * @param remainingPrincipal 剩余本金
     * @param annualRate 年利率（%）
     * @return 当月利息（元）
     */
    BigDecimal getMonthlyInterest(BigDecimal remainingPrincipal, BigDecimal annualRate);

    /**
     * 解析提前还款列表，转换为「月份-金额」映射
     * @param prepayments 提前还款列表
     * @return key=还款月份，value=提前还款金额
     */
    Map<Integer, BigDecimal> getAllPrepayMoney(List<Prepayment> prepayments);

    /**
     * 计算当月应还本金
     * @return  当月应还本金
     */
    BigDecimal getMonthlyPrincipal(int totalMonths, int nowMonth, BigDecimal remainingPrincipal);

    /**统计总还款信息*/
    CombinationLoanResponse getTotalResponse(EqualPrincipalRepayResponse businessResponse,
                                             EqualPrincipalRepayResponse fundResponse,
                                             CombinationLoanResponse  response);

    /**
     * 商贷计算
     */
    EqualPrincipalRepayResponse  businessResponse(CombinationLoanRequest request,
                                                  Map<Integer, BigDecimal> prepayMoney,
                                                  CombinationLoanResponse response, String type);

    /**
     * 公积金贷款计算
     */
    EqualPrincipalRepayResponse  fundResponse(CombinationLoanRequest request,
                                              Map<Integer, BigDecimal> prepayMoney,
                                              CombinationLoanResponse response,String type);
    /**
     * 计算贷款汇总
     * @param request  贷款请求参数
     * @param prepayMoney 提前还款信息
     * @return 还款汇总
     */
    EqualPrincipalRepayResponse calculatorPrincipal(EqualPrincipalRepayRequest request,
                                                    Map<Integer, BigDecimal> prepayMoney);
    /**
     * 构建单笔贷款请求参数
     */
    EqualPrincipalRepayRequest buildSingleLoanRequest(
            BigDecimal loanTotal,
            BigDecimal annualRate,
            Integer years,
            BigDecimal reservedPrincipal);
    /**
     *
     * @param month 当前月份
     * @param monthTotalPrincipal 月还款本金
     * @param monthlyInterest 月还款利息
     * @param remainingPrincipal 剩余未还款本金
     * @return 月度明细
     */
    EqualPrincipalRepayResponse.MonthlyDetail setMonthDetail(int month,
                                                                     BigDecimal monthTotalPrincipal,
                                                                     BigDecimal monthlyInterest,
                                                                     BigDecimal remainingPrincipal);

    /**
     * 封装年度信息
     * @param currentYear 当前年份
     * @param yearPrincipal 年本金
     * @param yearInterest 年利息
     * @return  年度账单信息
     */
    EqualPrincipalRepayResponse.YearSummary setYearDetail(int currentYear, BigDecimal yearPrincipal,
                                                                   BigDecimal yearInterest);
    /**
     * 按周期更新各月份的应还款金额（保留原有金额并累加）
     * @param prepayMoney 原有月份-金额映射（可为空，内部做兜底）
     * @param periodRepayList 周期还款列表（可为空，空则直接返回原数据）
     * @return 更新后的月份-金额映射
     */
    Map<Integer, BigDecimal> updatePayMoney(Map<Integer, BigDecimal> prepayMoney, List<PeriodRepay> periodRepayList);

    /**
     * 等额本息核心计算方法
     * @return 还款明细列表（包含每月数据，第一个元素为汇总信息，后续为每月明细）
     */
     EqualPrincipalRepayResponse calculateEqualInterestRepay(EqualPrincipalRepayRequest request,
                                                             Map<Integer, BigDecimal> prepayMoney);
}