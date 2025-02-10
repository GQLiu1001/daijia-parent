package com.atguigu.daijia.payment.receiver;

import com.alibaba.nacos.shaded.com.google.protobuf.Message;
import com.atguigu.daijia.common.constant.MqConst;
import com.atguigu.daijia.payment.service.WxPayService;
import jakarta.annotation.Resource;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.channels.Channel;

@Component
public class PaymentReceiver {


    @Resource
    private WxPayService wxPayService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_PAY_SUCCESS, durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_ORDER),
            key = {MqConst.ROUTING_PAY_SUCCESS}
    ))
    public void paySuccess(String orderNo) {
        System.out.println("触发了 PaymentReceiver");
        System.out.println("当前线程：" + Thread.currentThread().getName());
        System.out.println("接收的消息：" + orderNo);
        System.out.println("接收的队列：" + MqConst.QUEUE_PAY_SUCCESS);

        wxPayService.handleOrder(orderNo);
    }

}
