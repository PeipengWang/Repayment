package com.repay.service.impl;

import com.repay.entity.Prepayment;
import com.repay.service.RepayCalculator;
import com.repay.constant.CONSTANT;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
}