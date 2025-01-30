package com.atguigu.daijia.customer.service.impl;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.customer.service.OrderService;
import com.atguigu.daijia.map.client.MapFeignClient;
import com.atguigu.daijia.model.form.customer.ExpectOrderForm;
import com.atguigu.daijia.model.form.customer.SubmitOrderForm;
import com.atguigu.daijia.model.form.map.CalculateDrivingLineForm;
import com.atguigu.daijia.model.form.order.OrderInfoForm;
import com.atguigu.daijia.model.form.rules.FeeRuleRequest;
import com.atguigu.daijia.model.form.rules.FeeRuleRequestForm;
import com.atguigu.daijia.model.vo.customer.ExpectOrderVo;
import com.atguigu.daijia.model.vo.map.DrivingLineVo;
import com.atguigu.daijia.model.vo.rules.FeeRuleResponseVo;
import com.atguigu.daijia.order.client.OrderInfoFeignClient;
import com.atguigu.daijia.rules.client.FeeRuleFeignClient;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Service;

import java.util.Date;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderServiceImpl implements OrderService {
    @Resource
    private FeeRuleFeignClient feeClient;
    @Resource
    private MapFeignClient mapFeignClient;
    @Resource
    private OrderInfoFeignClient orderInfoFeignClient;
    @Override
    public ExpectOrderVo expectOrder(ExpectOrderForm expectOrderForm) {
        //获取驾驶路线
        CalculateDrivingLineForm form = new CalculateDrivingLineForm();
        BeanUtils.copyProperties(expectOrderForm, form);
        Result<DrivingLineVo> drivingLineVoResult = mapFeignClient.calculateDrivingLine(form);
        DrivingLineVo vo = drivingLineVoResult.getData();
        //获取订单费用 FeeRuleRequestForm需要DrivingLineVo.getDistance()
        FeeRuleRequestForm feeRuleRequestForm = new FeeRuleRequestForm();
        feeRuleRequestForm.setDistance(vo.getDistance());
        feeRuleRequestForm.setStartTime(new Date());
        feeRuleRequestForm.setWaitMinute(0);
        Result<FeeRuleResponseVo> feeRuleResponseVo = feeClient.calculateOrderFee(feeRuleRequestForm);
        FeeRuleResponseVo data = feeRuleResponseVo.getData();
        //封装返回ExpectOrderVo 需要DrivingLineVo和FeeRuleResponseVo
        ExpectOrderVo expectOrderVo = new ExpectOrderVo();
        expectOrderVo.setDrivingLineVo(vo);
        expectOrderVo.setFeeRuleResponseVo(data);
        return expectOrderVo;
    }

    @Override
    public Long submitOrder(SubmitOrderForm submitOrderForm) {
        //乘客下单
        //1.重新计算驾驶路线 点下单之前是预估
        CalculateDrivingLineForm form = new CalculateDrivingLineForm();
        BeanUtils.copyProperties(submitOrderForm, form);
        Result<DrivingLineVo> drivingLineVoResult = mapFeignClient.calculateDrivingLine(form);
        DrivingLineVo vo = drivingLineVoResult.getData();
        //2.重新获取订单费用 FeeRuleRequestForm需要DrivingLineVo.getDistance()
        FeeRuleRequestForm feeRuleRequestForm = new FeeRuleRequestForm();
        feeRuleRequestForm.setDistance(vo.getDistance());
        feeRuleRequestForm.setStartTime(new Date());
        feeRuleRequestForm.setWaitMinute(0);
        Result<FeeRuleResponseVo> feeRuleResponseVo = feeClient.calculateOrderFee(feeRuleRequestForm);
        FeeRuleResponseVo data = feeRuleResponseVo.getData();
        //封装数据
        OrderInfoForm orderInfoForm = new OrderInfoForm();
        BeanUtils.copyProperties(submitOrderForm, orderInfoForm);
        orderInfoForm.setExpectDistance(vo.getDistance());
        orderInfoForm.setExpectAmount(data.getTotalAmount());
        Result<Long> longResult = orderInfoFeignClient.saveOrderInfo(orderInfoForm);
        //TODO 查询附近可以接单的司机
        return longResult.getData();
    }
}
