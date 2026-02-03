package com.repay.entity.income;

import lombok.Getter;

/**
 * 银行存款类型枚举（活期/定期）
 */
@Getter
public enum DepositType {
    DEMAND("活期", 0), // 活期：按日计息
    FIXED("定期", 1); // 定期：到期一次性计息

    private final String desc;
    private final int type;

    DepositType(String desc, int type) {
        this.desc = desc;
        this.type = type;
    }
}