package com.repay.entity.EmergencyFund;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Schema(description = "年度统计数据")
public class AnnualStatDTO {
    @Schema(description = "年度平均应急能力（总覆盖月数/12）")
    private BigDecimal annualAvgEmergencyAbility;

    @Schema(description = "年度剩余资金（累计月度余额+存款利息）")
    private BigDecimal annualRemainingFund;

    @Schema(description = "单月应急金额度")
    private BigDecimal monthlyEmergencyQuota;

    @Schema(description = "基础应急能力金额额度（单月额度*N）")
    private BigDecimal baseEmergencyQuota;
}