package com.repay.controller;

import com.repay.entity.*;
import com.repay.service.RepayCalculator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.repay.constant.CONSTANT.ROUND_MODE;
import static com.repay.constant.CONSTANT.SCALE;

/**
 * 等额本金还款计算 Controller
 * POST 接口（JSON 参数）实现还款计算
 */
@RestController
@RequestMapping("/api/repay")
@Tag(name = "还款计算接口", description = "等额本金还款计算相关接口")
public class EqualInterestRepayController {
    // 金融计算精度：保留2位小数，四舍五入
    @Qualifier(value = "EqualPrincipalParamValidator")
    @Autowired
    public RepayCalculator repayCalculator;

    /**
     * 等额本金还款计算接口（POST 请求，JSON 传递参数）
     * @param request 贷款参数（JSON 格式）
     * @return 完整的还款计算结果（JSON 格式）
     */
    @PostMapping("/equal-interest")
    @Operation(summary = "等额本息还款计算", description = "POST请求-输入贷款总额、年利率、还款年限，返回每月/每年/总计还款信息")
    public CombinationLoanResponse calculateEqualPrincipal(
            @Valid @RequestBody CombinationLoanRequest request) {
        CombinationLoanResponse response = new CombinationLoanResponse();
        // 1. 分别计算商贷和公积金贷明细
        EqualPrincipalRepayResponse businessResponse = null;
        EqualPrincipalRepayResponse fundResponse = null;

        Map<Integer, BigDecimal> prepayMoney = new HashMap<>();
        if(!request.getPrepayments().isEmpty()){
            prepayMoney = repayCalculator.getAllPrepayMoney(request.getPrepayments());
        }
        //更新周期性还款方式
        prepayMoney = repayCalculator.updatePayMoney(prepayMoney, request.getPeriodicRepayList());


        //计算所有贷款（公积金+商贷）方式
        businessResponse = repayCalculator.businessResponse(request, prepayMoney, response, "equalInterest");
        fundResponse = repayCalculator.fundResponse(request, prepayMoney, response,"equalInterest");
        repayCalculator.getTotalResponse(businessResponse, fundResponse, response);
        return response;
    }

}