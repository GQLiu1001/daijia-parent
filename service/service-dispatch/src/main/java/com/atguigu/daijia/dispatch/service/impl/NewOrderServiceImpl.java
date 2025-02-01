package com.atguigu.daijia.dispatch.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson2.JSON;
import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.dispatch.mapper.OrderJobMapper;
import com.atguigu.daijia.dispatch.service.NewOrderService;
import com.atguigu.daijia.dispatch.xxl.client.XxlJobClient;
import com.atguigu.daijia.map.client.LocationFeignClient;
import com.atguigu.daijia.model.entity.dispatch.OrderJob;
import com.atguigu.daijia.model.enums.OrderStatus;
import com.atguigu.daijia.model.form.map.SearchNearByDriverForm;
import com.atguigu.daijia.model.vo.dispatch.NewOrderTaskVo;
import com.atguigu.daijia.model.vo.map.NearByDriverVo;
import com.atguigu.daijia.model.vo.order.NewOrderDataVo;
import com.atguigu.daijia.order.client.OrderInfoFeignClient;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class NewOrderServiceImpl implements NewOrderService {
    @Resource
    private XxlJobClient xxlJobClient;
    @Resource
    private OrderJobMapper orderJobMapper;
    //查看附近司机
    @Resource
    private LocationFeignClient locationFeignClient;
    //查询订单状态
    @Resource
    private OrderInfoFeignClient orderInfoFeignClient;
    @Resource
    private RedisTemplate redisTemplate;
    @Override
    public Long addAndStartTask(NewOrderTaskVo newOrderTaskVo) {
        LambdaQueryWrapper<OrderJob> queryWrapper = new LambdaQueryWrapper();
        queryWrapper.eq(OrderJob::getOrderId, newOrderTaskVo.getOrderId());
        OrderJob orderJob = orderJobMapper.selectOne(queryWrapper);
        if (orderJob == null) {
            //订单没开启任务调度
            //String executorHandler, String param, String corn, String desc
            //执行任务的job方法           相关参数 空      corn表达式      描述信息
            Long jobId = xxlJobClient.addAndStart("newOrderTaskHandler",
                    //corn可以先确定秒 再确定分钟方法
                    "", "0 */1 * * * ?",
                    "新建任务调度 id" + newOrderTaskVo.getOrderId());
            //记录任务调度信息
            orderJob = new OrderJob();
            orderJob.setOrderId(newOrderTaskVo.getOrderId());
            orderJob.setJobId(jobId);
            orderJob.setParameter(JSONObject.toJSONString(newOrderTaskVo));
            orderJobMapper.insert(orderJob);
        }
        return orderJob.getJobId();
    }

    //执行任务 搜索附近司机
    @Override
    public void executeTask(long jobId) {
        //根据jobId 查询数据库，看当前任务是否创建
        //如果没创建,不往下执行
        LambdaQueryWrapper<OrderJob> queryWrapper = new LambdaQueryWrapper();
        queryWrapper.eq(OrderJob::getJobId, jobId);
        OrderJob orderJob = orderJobMapper.selectOne(queryWrapper);
        if (orderJob == null) {
            return;//不往下执行
        }
        //查询订单状态 如果当前接单状态 继续执行 如果是停止接单的状态停止接单任务调度
        //拿到orderJob里的参数Parameter
        String jsonString = orderJob.getParameter();
        //把jsonString 转换为NewOrderTaskVo类型
        NewOrderTaskVo newOrderTaskVo = JSON.parseObject(jsonString, NewOrderTaskVo.class);
        //获取orderId
        Long orderId = newOrderTaskVo.getOrderId();
        //查看是否是接单状态
        Result<Integer> orderStatus = orderInfoFeignClient.getOrderStatus(orderId);
        Integer status = orderStatus.getData();
        if (status.intValue() != OrderStatus.WAITING_ACCEPT.getStatus().intValue()) {
            //停止调度
            xxlJobClient.stopJob(orderId);
        }
        //远程调用 搜索附近满足条件可以接单的司机
        SearchNearByDriverForm searchNearByDriverForm = new SearchNearByDriverForm();
        searchNearByDriverForm.setLongitude(newOrderTaskVo.getStartPointLongitude());
        searchNearByDriverForm.setLatitude(newOrderTaskVo.getStartPointLatitude());
        searchNearByDriverForm.setMileageDistance(newOrderTaskVo.getExpectDistance());
        //远程调用之后 获取满足接单的司机集合
        List<NearByDriverVo> nearByDriverVoList = locationFeignClient.searchNearByDriver().getData();
        //遍历司机集合，为每个司机创建临时队列 redis 存储新订单信息
        nearByDriverVoList.forEach(driver -> {
            //使用Redis的set类型
            //根据订单id生成key
            String repeatKey =
                    RedisConstant.DRIVER_ORDER_REPEAT_LIST+newOrderTaskVo.getOrderId();
            //记录司机id，防止重复推送
            Boolean isMember = redisTemplate.opsForSet().isMember(repeatKey, driver.getDriverId());
            if(!isMember) {
                //把订单信息推送给满足条件多个司机
                redisTemplate.opsForSet().add(repeatKey,driver.getDriverId());
                //过期时间：15分钟，超过15分钟没有接单自动取消
                redisTemplate.expire(repeatKey,
                        RedisConstant.DRIVER_ORDER_REPEAT_LIST_EXPIRES_TIME,
                        TimeUnit.MINUTES);
                NewOrderDataVo newOrderDataVo = new NewOrderDataVo();
                newOrderDataVo.setOrderId(newOrderTaskVo.getOrderId());
                newOrderDataVo.setStartLocation(newOrderTaskVo.getStartLocation());
                newOrderDataVo.setEndLocation(newOrderTaskVo.getEndLocation());
                newOrderDataVo.setExpectAmount(newOrderTaskVo.getExpectAmount());
                newOrderDataVo.setExpectDistance(newOrderTaskVo.getExpectDistance());
                newOrderDataVo.setExpectTime(newOrderTaskVo.getExpectTime());
                newOrderDataVo.setFavourFee(newOrderTaskVo.getFavourFee());
                newOrderDataVo.setDistance(driver.getDistance());
                newOrderDataVo.setCreateTime(newOrderTaskVo.getCreateTime());
                //新订单保存司机的临时队列，Redis里面List集合
                String key = RedisConstant.DRIVER_ORDER_TEMP_LIST+driver.getDriverId();
                redisTemplate.opsForList().leftPush(key,JSONObject.toJSONString(newOrderDataVo));
                //过期时间：1分钟
                redisTemplate.expire(key,RedisConstant.DRIVER_ORDER_TEMP_LIST_EXPIRES_TIME, TimeUnit.MINUTES);
            }
        });
    }

    //获取最新订单
    @Override
    public List<NewOrderDataVo> findNewOrderQueueData(Long driverId) {
        List<NewOrderDataVo> list = new ArrayList<>();
        String key = RedisConstant.DRIVER_ORDER_TEMP_LIST + driverId;
        Long size = redisTemplate.opsForList().size(key);
        if(size > 0) {
            for (int i = 0; i < size; i++) {
                String content = (String)redisTemplate.opsForList().leftPop(key);
                NewOrderDataVo newOrderDataVo = JSONObject.parseObject(content,NewOrderDataVo.class);
                list.add(newOrderDataVo);
            }
        }
        return list;
    }

    //清空队列数据
    @Override
    public Boolean clearNewOrderQueueData(Long driverId) {
        String key = RedisConstant.DRIVER_ORDER_TEMP_LIST + driverId;
        redisTemplate.delete(key);
        return true;
    }

}
