package com.repay.entity.EmergencyFund;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "月度统计数据")
public class MonthlyStatDTO {
    @Schema(description = "统计月份")
    private LocalDate statMonth;

    @Schema(description = "每月支出（必要支出+贷款月供）")
    private BigDecimal monthlyExpense;

    @Schema(description = "每月收入（工资+其他收入）")
    private BigDecimal monthlyIncome;

    @Schema(description = "月度余额（收入-支出）")
    private BigDecimal monthlyBalance;

    @Schema(description = "月度应急覆盖月数")
    private BigDecimal emergencyCoverageMonths;
}