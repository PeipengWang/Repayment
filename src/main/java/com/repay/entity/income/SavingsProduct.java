package com.repay.entity.income;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 储蓄产品实体（单条储蓄记录）
 */
@Data
public class SavingsProduct {
    /** 储蓄类型（银行存款/债券/基金/股票） */
    private SavingsType savingsType;
    /** 产品名称（如：招商银行定期存款、易方达基金） */
    private String productName;
    /** 本金金额（元） */
    private BigDecimal principal;
    /** 年利率（%）：活期/定期/债券/基金通用，股票可设为月度收益率 */
    private BigDecimal annualRate;
    /** 存款/购买日期（格式：yyyy-MM-dd） */
    private LocalDate startDate;
    /** 到期日期（仅定期存款/债券需要，活期为null） */
    private LocalDate endDate;
    /** 存款类型（仅银行存款需要：活期/定期） */
    private DepositType depositType;
    /** 月度收益率（仅股票/基金可选，优先级高于年利率） */
    private BigDecimal monthlyRate;
    /** 定期时间*/
    private int fixedTermYears = 1;
}
