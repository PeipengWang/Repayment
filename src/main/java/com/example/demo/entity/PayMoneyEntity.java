package com.example.demo.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

import static com.example.demo.constant.CONSTANT.ROUND_MODE;
import static com.example.demo.constant.CONSTANT.SCALE;

@Data
@Accessors(chain = true)
public class PayMoneyEntity {
    private BigDecimal loanTotal; //需要还总金额
    private BigDecimal annualRate; //利率
    private BigDecimal monthlyPrincipal; //每月还款金额
    private BigDecimal totalAllPrincipal = BigDecimal.ZERO; //已还总本金
    private BigDecimal totalAllInterest = BigDecimal.ZERO; //已还总利息
    private BigDecimal yearPrincipal = BigDecimal.ZERO; //年总本金
    private BigDecimal yearInterest = BigDecimal.ZERO; //年总利息

    private int totalMonths; //需要还的总月数
    private BigDecimal remainingPrincipal = BigDecimal.ZERO;

}
