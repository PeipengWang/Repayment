package com.repay.entity.income;

import lombok.Getter;

/**
 * 储蓄类型枚举
 */
@Getter
public enum SavingsType {
    BANK_DEPOSIT("银行存款"),
    BOND("债券"),
    FUND("基金"),
    STOCK("股票");

    private final String desc;

    SavingsType(String desc) {
        this.desc = desc;
    }
}

