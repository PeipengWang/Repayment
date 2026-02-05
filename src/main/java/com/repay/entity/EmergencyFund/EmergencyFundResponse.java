package com.repay.entity.EmergencyFund;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Data
@Schema(description = "应急金计算响应结果")
public class EmergencyFundResponse {
    @Schema(description = "年度统计信息")
    private AnnualStatDTO annualStat;

    @Schema(description = "月度统计列表")
    private List<MonthlyStatDTO> monthlyStatList;
}