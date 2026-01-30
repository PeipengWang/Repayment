package com.example.demo.controller;

import com.example.demo.entity.EqualPrincipalRepayRequest;
import com.example.demo.entity.EqualPrincipalRepayResponse;
import com.example.demo.entity.Prepayment;
import com.example.demo.entity.RepayCalculationState;
import com.example.demo.service.RepayCalculator;
import com.sun.source.tree.UsesTree;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.models.security.SecurityScheme;
import jakarta.annotation.Resource;
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

import static com.example.demo.constant.CONSTANT.*;

/**
 * 等额本金还款计算 Controller
 * POST 接口（JSON 参数）实现还款计算
 */
@RestController
@RequestMapping("/api/repay")
@Tag(name = "还款计算接口", description = "等额本金还款计算相关接口")
public class EqualPrincipalRepayController {
    // 金融计算精度：保留2位小数，四舍五入
    @Qualifier(value = "EqualPrincipalParamValidator")
    @Autowired
    public RepayCalculator repayCalculator;

    /**
     * 等额本金还款计算接口（POST 请求，JSON 传递参数）
     * @param request 贷款参数（JSON 格式）
     * @return 完整的还款计算结果（JSON 格式）
     */
    @PostMapping("/equal-principal")
    @Operation(summary = "等额本金还款计算", description = "POST请求-输入贷款总额、年利率、还款年限，返回每月/每年/总计还款信息")
    public EqualPrincipalRepayResponse calculateEqualPrincipal(
            @Valid @RequestBody EqualPrincipalRepayRequest request) {
        RepayCalculationState repayCalculationState = new RepayCalculationState();
        // 1. 获取请求参数
        BigDecimal annualRate = request.getAnnualRate();
        BigDecimal loanTotal = request.getLoanTotal().setScale(SCALE, ROUND_MODE);
        Integer years = request.getYears();
        int totalMonths = years * 12;  //需要还款总月数
        //首月还款数
        BigDecimal monthlyPrincipal = loanTotal.divide(new BigDecimal(totalMonths), SCALE, ROUND_MODE);
        //剩余本金
        BigDecimal remainingPrincipal = loanTotal;
        //获取所有提前还款年份
        Map<Integer, BigDecimal> prepayMoney = new HashMap<>();
        if(!request.getPrepayments().isEmpty()){
            prepayMoney = repayCalculator.getAllPrepayMoney(request.getPrepayments());
        }
        // 4. 初始化统计变量和结果列表

        List<EqualPrincipalRepayResponse.MonthlyDetail> monthlyDetails = new ArrayList<>();
        List<EqualPrincipalRepayResponse.YearSummary> yearSummaries = new ArrayList<>();

        BigDecimal totalAllPrincipal = BigDecimal.ZERO; //已还总本金
        BigDecimal totalAllInterest = BigDecimal.ZERO; //已还总利息
        BigDecimal yearPrincipal = BigDecimal.ZERO; //年总本金
        BigDecimal yearInterest = BigDecimal.ZERO; //年总利息

        Integer period = request.getPeriod(); //还款周期
        // 5. 遍历每月计算明细
        for (int month = 1; month <= totalMonths; month++) {
            // 计算当月利息和还款额
            BigDecimal monthlyInterest = repayCalculator.getMonthlyInterest(remainingPrincipal, annualRate);
            // 更新剩余本金
            remainingPrincipal = remainingPrincipal.subtract(monthlyPrincipal).setScale(SCALE, ROUND_MODE);
            if (remainingPrincipal.compareTo(BigDecimal.ZERO) < 0) {
                remainingPrincipal = BigDecimal.ZERO;
            }
            BigDecimal prepayMoneyCurrentMonth = BigDecimal.ZERO;
            BigDecimal monthTotalPrincipal = monthlyPrincipal;
            if(month % period == 0){
//                prepayMoneyCurrentMonth = prepayMoneyCurrentMonth.add(request.getPeriodPay());
            }
            if(prepayMoney.containsKey(month) || !prepayMoneyCurrentMonth.equals(BigDecimal.ZERO)){
                prepayMoneyCurrentMonth = prepayMoney.get(month);
                remainingPrincipal = remainingPrincipal.subtract(prepayMoney.get(month));
                monthTotalPrincipal = monthlyPrincipal.add(prepayMoneyCurrentMonth);
                //重新计算每月还款本金
                monthlyPrincipal = remainingPrincipal.divide(new BigDecimal(totalMonths).subtract(new BigDecimal(month)),
                        SCALE, ROUND_MODE);
            }
            // 封装当月明细
            monthlyDetails.add(setMonthDetail(month, monthTotalPrincipal, monthlyInterest, remainingPrincipal));
            // 累加统计数据
            yearPrincipal = yearPrincipal.add(monthTotalPrincipal);
            yearInterest = yearInterest.add(monthlyInterest);
            totalAllPrincipal = totalAllPrincipal.add(monthTotalPrincipal);
            totalAllInterest = totalAllInterest.add(monthlyInterest);

            // 每年结束时封装年度汇总
            if (month % 12 == 0) {
                int currentYear = month / 12;
                yearSummaries.add(setYearDetail(currentYear, yearPrincipal, yearInterest));
                // 重置当年统计变量
                yearPrincipal = BigDecimal.ZERO;
                yearInterest = BigDecimal.ZERO;
            }
        }
        // 6. 封装总计信息
        BigDecimal totalAllRepay = totalAllPrincipal.add(totalAllInterest);
        return new EqualPrincipalRepayResponse()
                .setMonthlyDetails(monthlyDetails)
                .setYearSummaries(yearSummaries)
                .setTotalAllPrincipal(totalAllPrincipal)
                .setTotalAllInterest(totalAllInterest)
                .setTotalAllRepay(totalAllRepay)
                .setLoanTotal(loanTotal)
                .setAnnualRate(annualRate)
                .setYears(years)
                .setTotalMonths(totalMonths);
    }

    /**
     *
     * @param month 当前月份
     * @param monthTotalPrincipal 月还款本金
     * @param monthlyInterest 月还款利息
     * @param remainingPrincipal 剩余未还款本金
     * @return 月度明细
     */
    private EqualPrincipalRepayResponse.MonthlyDetail setMonthDetail(int month,
                                                                     BigDecimal monthTotalPrincipal,
                                                                     BigDecimal monthlyInterest,
                                                                     BigDecimal remainingPrincipal){
        EqualPrincipalRepayResponse.MonthlyDetail monthlyDetail = new EqualPrincipalRepayResponse.MonthlyDetail();
        monthlyDetail.setMonth(month);
        monthlyDetail.setMonthlyPrincipal(monthTotalPrincipal);
        monthlyDetail.setMonthlyInterest(monthlyInterest);
        monthlyDetail.setMonthlyRepay(monthlyInterest.add(monthTotalPrincipal));
        monthlyDetail.setRemainingPrincipal(remainingPrincipal);
        return monthlyDetail;
    }

    /**
     * 封装年度信息
     * @param currentYear 当前年份
     * @param yearPrincipal 年本金
     * @param yearInterest 年利息
     * @return  年度账单信息
     */
    private  EqualPrincipalRepayResponse.YearSummary setYearDetail(int currentYear, BigDecimal yearPrincipal,
                                                                   BigDecimal yearInterest){
        EqualPrincipalRepayResponse.YearSummary yearSummary = new EqualPrincipalRepayResponse.YearSummary();
        yearSummary.setYear(currentYear);
        yearSummary.setYearPrincipal(yearPrincipal);
        yearSummary.setYearInterest(yearInterest);
        yearSummary.setYearTotalRepay(yearInterest.add(yearPrincipal));
        return yearSummary;
    }
}