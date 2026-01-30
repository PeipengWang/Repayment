package com.repay.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Data
@Accessors(chain = true)
public class PeriodRepay {
    private Integer cycleMonths;
    private Integer startMonth;
    private Integer endMonth;
    private BigDecimal amount;
}
