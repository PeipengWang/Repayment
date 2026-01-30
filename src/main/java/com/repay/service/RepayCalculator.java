package com.repay.service;

import com.repay.entity.Prepayment;

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
}