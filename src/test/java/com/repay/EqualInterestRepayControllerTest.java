//package com.repay;
//
//import com.repay.controller.EqualInterestRepayController;
//import com.repay.entity.*;
//import com.repay.service.RepayCalculator;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.validation.BindingResult;
//
//import java.math.BigDecimal;
//import java.util.*;
//
//import static com.repay.constant.CONSTANT.SCALE;
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.mockito.ArgumentMatchers.anyList;
//import static org.mockito.Mockito.*;
//
///**
// * 等额本息还款计算Controller测试用例（修正版）
// * 覆盖：纯商贷/纯公积金/组合贷 + 无提前还款/单次提前还款/周期性提前还款
// */
//@ExtendWith(MockitoExtension.class)
//class EqualInterestRepayControllerTest {
//
//    @InjectMocks
//    private EqualInterestRepayController controller;
//
//    @Mock
//    private RepayCalculator repayCalculator;
//
//    private static final BigDecimal RESERVED_PRINCIPAL = BigDecimal.ZERO;
//    private static final BigDecimal BUSINESS_ANNUAL_RATE = new BigDecimal("4.25");
//    private static final BigDecimal FUND_ANNUAL_RATE = new BigDecimal("2.6");
//    private static final Integer LOAN_YEARS = 1; // 12个月
//    private static final BigDecimal BUSINESS_LOAN_TOTAL = new BigDecimal("120000");
//    private static final BigDecimal FUND_LOAN_TOTAL = new BigDecimal("60000");
//
//    private final BindingResult bindingResult = mock(BindingResult.class);
//
//    @BeforeEach
//    void setUp() {
//        when(bindingResult.hasErrors()).thenReturn(false);
//    }
//
//    // ===================== 场景1：纯商贷 - 无提前还款 =====================
//    @Test
//    void calculateEqualInterest_SingleBusinessLoan_NoPrepay() {
//        CombinationLoanRequest request = buildBaseRequest();
//        request.setLoanType("single");
//        request.setPrepayments(Collections.emptyList());
//        request.setPeriodicRepayList(Collections.emptyList());
//
//        when(repayCalculator.getAllPrepayMoney(anyList())).thenReturn(new HashMap<>());
//
//        CombinationLoanResponse response = controller.calculateEqualPrincipal(request);
//
//        assertThat(response).isNotNull();
//        assertThat(response.getBusinessMonthlyDetails()).isNotEmpty();
//        assertThat(response.getFundMonthlyDetails()).isNull();
//        assertThat(response.getTotalMonths()).isEqualTo(12);
//        assertThat(response.getTotalAllPrincipal()).isEqualByComparingTo(BUSINESS_LOAN_TOTAL);
//        assertThat(response.getTotalAllInterest()).isGreaterThan(BigDecimal.ZERO);
//        assertThat(response.getTotalAllRepay())
//                .isEqualByComparingTo(response.getTotalAllPrincipal().add(response.getTotalAllInterest()));
//
//        // 验证前11个月月供基本一致（允许极小误差）
//        List<EqualPrincipalRepayResponse.MonthlyDetail> details = response.getBusinessMonthlyDetails();
//        assertThat(details).hasSize(12);
//        BigDecimal firstRepay = details.get(0).getMonthlyRepay();
//        for (int i = 1; i < 11; i++) {
//            assertThat(details.get(i).getMonthlyRepay().subtract(firstRepay).abs())
//                    .isLessThanOrEqualTo(new BigDecimal("0.01")); // 允许1分钱误差
//        }
//        // 最后一期剩余本金清零
//        assertThat(details.get(11).getRemainingPrincipal()).isEqualByComparingTo(BigDecimal.ZERO);
//    }
//
//    // ===================== 场景2：纯公积金贷 - 第6个月提前还款10000元 =====================
//    @Test
//    void calculateEqualInterest_SingleFundLoan_SinglePrepay() {
//        CombinationLoanRequest request = buildBaseRequest();
//        request.setLoanType("fund");
//        request.setBusinessLoanTotal(BigDecimal.ZERO); // 清除商贷
//        request.setFundLoanTotal(FUND_LOAN_TOTAL);
//        request.setFundAnnualRate(FUND_ANNUAL_RATE);
//        request.setFundYears(LOAN_YEARS);
//
//        Prepayment prepayment = new Prepayment();
//        prepayment.setMonth(6);
//        prepayment.setAmount(new BigDecimal("10000")); // ✅ 修正：原为0！
//        request.setPrepayments(Collections.singletonList(prepayment));
//        request.setPeriodicRepayList(Collections.emptyList());
//
//        Map<Integer, BigDecimal> prepayMap = new HashMap<>();
//        prepayMap.put(6, new BigDecimal("10000"));
//        when(repayCalculator.getAllPrepayMoney(anyList())).thenReturn(prepayMap);
//
//        CombinationLoanResponse response = controller.calculateEqualPrincipal(request);
//
//        assertThat(response).isNotNull();
//        assertThat(response.getFundMonthlyDetails()).isNotEmpty();
//        assertThat(response.getBusinessMonthlyDetails()).isNull();
//        assertThat(response.getTotalAllPrincipal()).isEqualByComparingTo(FUND_LOAN_TOTAL);
//
//        List<EqualPrincipalRepayResponse.MonthlyDetail> fundDetails = response.getFundMonthlyDetails();
//        assertThat(fundDetails).hasSize(12);
//
//        // 第6个月（index=5）本金包含1万提前还款
//        EqualPrincipalRepayResponse.MonthlyDetail sixth = fundDetails.get(5);
//        assertThat(sixth.getMonthlyPrincipal()).isGreaterThanOrEqualTo(new BigDecimal("10000"));
//
//        // 提前还款后，第7个月月供应下降
//        BigDecimal repayBefore = fundDetails.get(4).getMonthlyRepay(); // 第5个月
//        BigDecimal repayAfter = fundDetails.get(6).getMonthlyRepay();  // 第7个月
//        assertThat(repayAfter).isLessThan(repayBefore);
//
//        // 总利息应小于无提前还款场景（可选：需预先计算基准值）
//        assertThat(response.getTotalAllInterest()).isGreaterThan(BigDecimal.ZERO);
//    }
//
//    // ===================== 场景3：组合贷 - 每3个月提前还款5000（1/4/7/10月） =====================
//    @Test
//    void calculateEqualInterest_CombinationLoan_PeriodicPrepay() {
//        CombinationLoanRequest request = buildBaseRequest();
//        request.setLoanType("combination");
//        request.setPrepayments(Collections.emptyList());
//
//        PeriodRepay periodRepay = new PeriodRepay();
//        periodRepay.setStartMonth(1);
//        periodRepay.setEndMonth(12);
//        periodRepay.setCycleMonths(3);
//        periodRepay.setAmount(new BigDecimal("5000"));
//        request.setPeriodicRepayList(Collections.singletonList(periodRepay));
//
//        // ✅ 关键：Mock周期性还款转换逻辑（假设Controller会调用此方法）
//        Map<Integer, BigDecimal> expectedPrepayMap = new HashMap<>();
//        expectedPrepayMap.put(1, new BigDecimal("5000"));
//        expectedPrepayMap.put(4, new BigDecimal("5000"));
//        expectedPrepayMap.put(7, new BigDecimal("5000"));
//        expectedPrepayMap.put(10, new BigDecimal("5000"));
//        when(repayCalculator.getAllPrepayMoney(anyList())).thenReturn(new HashMap<>()); // 单次为空
//        // 如果Controller内部调用另一个方法处理周期性，需Mock它，例如：
//        // when(repayCalculator.convertPeriodicToMap(anyList())).thenReturn(expectedPrepayMap);
//        // 但根据你代码，似乎由Controller自己处理？此处假设 getAllPrepayMoney 已包含全部
//
//        // 为简化，我们直接让 getAllPrepayMoney 返回完整Map（模拟Controller已合并）
//        when(repayCalculator.getAllPrepayMoney(anyList())).thenReturn(expectedPrepayMap);
//
//        CombinationLoanResponse response = controller.calculateEqualPrincipal(request);
//
//        assertThat(response).isNotNull();
//        assertThat(response.getBusinessMonthlyDetails()).isNotEmpty();
//        assertThat(response.getFundMonthlyDetails()).isNotEmpty();
//        assertThat(response.getTotalAllPrincipal())
//                .isEqualByComparingTo(BUSINESS_LOAN_TOTAL.add(FUND_LOAN_TOTAL));
//
//        // 合并月度明细（假设Controller已合并）
//        List<EqualPrincipalRepayResponse.MonthlyDetail> merged = response.getMonthlyDetails();
//        assertThat(merged).hasSize(12);
//
//        int[] prepayMonths = {0, 3, 6, 9}; // 对应1/4/7/10月（0-based）
//        for (int idx : prepayMonths) {
//            // 提前还款月的本金显著高于下一个月
//            BigDecimal thisMonthPrincipal = merged.get(idx).getMonthlyPrincipal();
//            BigDecimal nextMonthPrincipal = merged.get(idx + 1).getMonthlyPrincipal();
//            assertThat(thisMonthPrincipal).isGreaterThan(nextMonthPrincipal);
//        }
//
//        assertThat(response.getBusinessYearSummaries()).hasSize(1);
//        assertThat(response.getFundYearSummaries()).hasSize(1);
//    }
//
//    // ===================== 边界场景：贷款总额为0 =====================
//    @Test
//    void calculateEqualInterest_ZeroLoanTotal_ReturnsZero() {
//        CombinationLoanRequest request = buildBaseRequest();
//        request.setLoanType("single");
//        request.setBusinessLoanTotal(BigDecimal.ZERO);
//        request.setFundLoanTotal(BigDecimal.ZERO);
//        request.setPrepayments(Collections.emptyList());
//        request.setPeriodicRepayList(Collections.emptyList());
//
//        when(repayCalculator.getAllPrepayMoney(anyList())).thenReturn(new HashMap<>());
//
//        CombinationLoanResponse response = controller.calculateEqualPrincipal(request);
//
//        assertThat(response.getTotalAllPrincipal()).isEqualByComparingTo(BigDecimal.ZERO);
//        assertThat(response.getTotalAllInterest()).isEqualByComparingTo(BigDecimal.ZERO);
//        assertThat(response.getTotalAllRepay()).isEqualByComparingTo(BigDecimal.ZERO);
//        assertThat(response.getMonthlyDetails()).isEmpty();
//    }
//
//    // ===================== 辅助方法 =====================
//    private CombinationLoanRequest buildBaseRequest() {
//        CombinationLoanRequest request = new CombinationLoanRequest();
//        request.setBusinessLoanTotal(BUSINESS_LOAN_TOTAL);
//        request.setBusinessAnnualRate(BUSINESS_ANNUAL_RATE);
//        request.setBusinessYears(LOAN_YEARS);
//        request.setFundLoanTotal(FUND_LOAN_TOTAL);
//        request.setFundAnnualRate(FUND_ANNUAL_RATE);
//        request.setFundYears(LOAN_YEARS);
//        request.setReservedPrincipal(RESERVED_PRINCIPAL);
//        request.setPrepayments(new ArrayList<>());
//        request.setPeriodicRepayList(new ArrayList<>());
//        return request;
//    }
//}