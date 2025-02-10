package com.atguigu.daijia.order.handle;

import com.atguigu.daijia.common.util.AuthContextHolder;
import com.atguigu.daijia.order.service.OrderInfoService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

//监听延迟队列
@Component
public class RedisDelayHandle {

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private OrderInfoService orderInfoService;

    @PostConstruct
    public void listener() {

        System.out.println("PostConstruct listener() 方法被调用了!"); // 再次确认 @PostConstruct 被调用 (保留这个日志)
        new Thread(()->{
            System.out.println("监听线程已启动!"); //  添加： 确认监听线程是否启动
            while(true) {
                RBlockingQueue<String> blockingQueue = redissonClient.getBlockingQueue("queue_cancel");
                System.out.println("监听线程准备从队列 queue_cancel 获取消息..."); // 添加： 循环开始，准备获取消息
                try {
                    String orderIdStr = blockingQueue.take();
                    System.out.println("监听线程从队列 queue_cancel 获取到消息，消息内容为: {}" + orderIdStr); // 打印取出的消息 (保留这个日志)

                    if(StringUtils.hasText(orderIdStr)) {
                        System.out.println("监听线程准备取消订单，orderId: {}" + orderIdStr); // 添加： 准备取消订单
                        orderInfoService.cusDrop(Long.parseLong(orderIdStr));
                        System.out.println("监听线程订单取消完成，orderId: {}" + orderIdStr); // 添加： 订单取消完成
                    }

                } catch (InterruptedException e) {
                    System.err.println("监听线程被中断，异常信息: {}" + e.getMessage()); // 添加： 捕获中断异常
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }
}
