package com.atguigu.daijia.payment.service.impl;

import com.alibaba.fastjson2.JSON;
import com.atguigu.daijia.common.constant.MqConst;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.common.service.RabbitService;
import com.atguigu.daijia.coupon.client.CouponFeignClient;
import com.atguigu.daijia.customer.client.CustomerInfoFeignClient;
import com.atguigu.daijia.driver.client.DriverAccountFeignClient;
import com.atguigu.daijia.driver.client.DriverInfoFeignClient;
import com.atguigu.daijia.map.client.WxPayFeignClient;
import com.atguigu.daijia.model.entity.order.OrderInfo;
import com.atguigu.daijia.model.entity.payment.PaymentInfo;
import com.atguigu.daijia.model.enums.OrderStatus;
import com.atguigu.daijia.model.enums.TradeType;
import com.atguigu.daijia.model.form.coupon.UseCouponForm;
import com.atguigu.daijia.model.form.driver.TransferForm;
import com.atguigu.daijia.model.form.payment.CreateWxPaymentForm;
import com.atguigu.daijia.model.form.payment.PaymentInfoForm;
import com.atguigu.daijia.model.vo.map.DrivingLineVo;
import com.atguigu.daijia.model.vo.order.OrderPayVo;
import com.atguigu.daijia.model.vo.order.OrderRewardVo;
import com.atguigu.daijia.model.vo.payment.WxPrepayVo;
import com.atguigu.daijia.order.client.OrderInfoFeignClient;
import com.atguigu.daijia.payment.mapper.PaymentInfoMapper;
import com.atguigu.daijia.payment.service.WxPayService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.partnerpayments.app.model.Amount;
import com.wechat.pay.java.service.partnerpayments.app.model.PrepayRequest;
import com.wechat.pay.java.service.partnerpayments.jsapi.JsapiServiceExtension;
import io.seata.spring.annotation.GlobalTransactional;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.util.RequestUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.wechat.pay.java.service.payments.model.Transaction;

import java.math.BigDecimal;
import java.util.Date;

@Service
@Slf4j
public class WxPayServiceImpl implements WxPayService {
    @Resource
    private PaymentInfoMapper paymentInfoMapper;
    @Resource
    private OrderInfoFeignClient infoFeignClient;
    @Resource
    private CustomerInfoFeignClient customerInfoFeignClient;
    @Resource
    private DriverInfoFeignClient driverInfoFeignClient;
    @Resource
    private CouponFeignClient couponFeignClient;
    @Resource
    private WxPayFeignClient wxPayFeignClient;
    @Resource
    private OrderInfoFeignClient orderInfoFeignClient;
    @Resource
    private DriverAccountFeignClient driverAccountFeignClient;
    @Resource
    private RabbitService rabbitService;
    @Resource
    private RedisTemplate drivingLineRedisTemplate;

    @Override
    public WxPrepayVo createWxPayment(PaymentInfoForm paymentInfoForm) {
        //1 添加支付记录到支付表里面
        //判断：如果表存在订单支付记录，不需要添加
        LambdaQueryWrapper<PaymentInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentInfo::getOrderNo, paymentInfoForm.getOrderNo());
        PaymentInfo paymentInfo = paymentInfoMapper.selectOne(wrapper);
        if (paymentInfo == null) {
            paymentInfo = new PaymentInfo();
            BeanUtils.copyProperties(paymentInfoForm, paymentInfo);
            paymentInfo.setPaymentStatus(0);
            paymentInfoMapper.insert(paymentInfo);
        }
        String orderNo = paymentInfo.getOrderNo();
        System.out.println("得到的orderNo=" + orderNo);
        infoFeignClient.updateOrderFinally(orderNo);
        WxPrepayVo wxPrepayVo = new WxPrepayVo();
        wxPrepayVo.setAppId("appId"); //公众号ID
        wxPrepayVo.setNonceStr("nonceStr" + paymentInfoForm.getOrderNo()); //随机串
        wxPrepayVo.setPaySign("paySign"); //微信签名
        wxPrepayVo.setSignType("signType"); //微信签名方式
        wxPrepayVo.setTimeStamp("timeStamp" + paymentInfoForm.getOrderNo()); //时间戳，自1970年以来的秒数
        wxPrepayVo.setPackageVal("packageVal" + paymentInfoForm.getOrderNo()); //预支付交易会话标识
        return wxPrepayVo;
    }

    //查询支付状态
    @Override
    public Boolean queryPayStatus(String orderNo) {
        System.out.println("微信支付成功后，进行的回调");
        Transaction transaction = new Transaction();
        transaction.setOutTradeNo(orderNo);
        transaction.setTransactionId("setTransactionId" + orderNo);
        handlePayment(transaction);
        return true;
    }

    //如果支付成功，调用其他方法实现支付后处理逻辑
    public void handlePayment(Transaction transaction) {
        System.out.println("----------------------------------------------------");
        System.out.println("调用了如果支付成功，调用此方法实现支付后处理逻辑");
        //1 更新支付记录，状态修改为 已经支付
        //订单编号
        String orderNo = transaction.getOutTradeNo();
        System.out.println("得到的orderId=" + orderNo);
        //根据订单编号查询支付记录
        LambdaQueryWrapper<PaymentInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentInfo::getOrderNo, orderNo);
        PaymentInfo paymentInfo = paymentInfoMapper.selectOne(wrapper);
        //如果已经支付，不需要更新
        if (paymentInfo.getPaymentStatus() == 1) {
            return;
        }
        paymentInfo.setPaymentStatus(1);
//        paymentInfo.setOrderNo(transaction.getOutTradeNo());
        paymentInfo.setTransactionId(transaction.getTransactionId());
        paymentInfo.setCallbackTime(new Date());
        paymentInfo.setCallbackContent(JSON.toJSONString(transaction));
        // 发送 MQ 消息
        System.out.println("发送 MQ 消息：");
        System.out.println("Exchange: " + MqConst.EXCHANGE_ORDER);
        System.out.println("Routing Key: " + MqConst.ROUTING_PAY_SUCCESS);
        System.out.println("Message: " + orderNo);
        paymentInfoMapper.updateById(paymentInfo);
        //2 发送端：发送mq消息，传递 订单编号
        //  接收端：获取订单编号，完成后续处理
        rabbitService.sendMessage(MqConst.EXCHANGE_ORDER,
                MqConst.ROUTING_PAY_SUCCESS,
                orderNo);
    }


    //支付成功后续处理
    @GlobalTransactional //分布式事务 seata
    @Override
    public void handleOrder(String orderNo) {
        System.out.println("-------------------------------------------------------------------");
        System.out.println("触发了支付成功后续处理，订单号：" + orderNo);
        System.out.println("-------------------------------------------------------------------");

        System.out.println("获取系统奖励...");
        OrderRewardVo orderRewardVo = orderInfoFeignClient.getOrderRewardFee(orderNo).getData();
        System.out.println("系统奖励内容：" + orderRewardVo);

        if (orderRewardVo != null && orderRewardVo.getRewardFee().doubleValue() > 0) {
            System.out.println("奖励金额：" + orderRewardVo.getRewardFee());
            TransferForm transferForm = new TransferForm();
            transferForm.setTradeNo(orderNo);
            transferForm.setTradeType(TradeType.REWARD.getType());
            transferForm.setContent(TradeType.REWARD.getContent());
            transferForm.setAmount(orderRewardVo.getRewardFee());
            transferForm.setDriverId(orderRewardVo.getDriverId());
            System.out.println("调用司机账户转账接口...");
            driverAccountFeignClient.transfer(transferForm);
        }

        System.out.println("增加司机订单数...");
        Result<OrderInfo> orderInfoByOrderNo = orderInfoFeignClient.getOrderInfoByOrderNo(orderNo);
        OrderInfo orderInfo = orderInfoByOrderNo.getData();
        Long userId = orderInfo.getCustomerId();
        Long driverId = orderInfo.getDriverId();
        driverInfoFeignClient.increaseOrderCount(driverId);
        System.out.println("司机订单数增加成功，司机 ID：" + driverId);
        System.out.println("从redis删除drivingLineVO 离谱的bug");
        drivingLineRedisTemplate.delete("begin_forCus_drivingLineVo"+userId);
    }

}