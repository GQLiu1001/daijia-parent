package com.atguigu.daijia.order.service.impl;

import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.model.entity.order.OrderBill;
import com.atguigu.daijia.model.entity.order.OrderInfo;
import com.atguigu.daijia.model.entity.order.OrderProfitsharing;
import com.atguigu.daijia.model.entity.order.OrderStatusLog;
import com.atguigu.daijia.model.enums.OrderStatus;
import com.atguigu.daijia.model.form.order.OrderInfoForm;
import com.atguigu.daijia.model.form.order.StartDriveForm;
import com.atguigu.daijia.model.form.order.UpdateOrderBillForm;
import com.atguigu.daijia.model.form.order.UpdateOrderCartForm;
import com.atguigu.daijia.model.vo.base.PageVo;
import com.atguigu.daijia.model.vo.order.*;
import com.atguigu.daijia.order.mapper.OrderBillMapper;
import com.atguigu.daijia.order.mapper.OrderInfoMapper;
import com.atguigu.daijia.order.mapper.OrderProfitsharingMapper;
import com.atguigu.daijia.order.mapper.OrderStatusLogMapper;
import com.atguigu.daijia.order.service.OrderInfoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mysql.cj.x.protobuf.MysqlxCrud;
import io.lettuce.core.ScriptOutputType;
import io.swagger.v3.oas.models.security.SecurityScheme;
import jakarta.annotation.Resource;
import lombok.Data;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {
    @Resource
    private OrderInfoMapper orderInfoMapper;
    @Resource
    private OrderStatusLogMapper orderStatusLogMapper;
    @Resource
    private RedisTemplate redisTemplate;
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private OrderProfitsharingMapper orderProfitsharingMapper;
    @Resource
    private OrderBillMapper orderBillMapper;
    @Override
    public Long saveOrderInfo(OrderInfoForm orderInfoForm) {
        //向OrderInfo表添加数据
        OrderInfo orderInfo = new OrderInfo();
        BeanUtils.copyProperties(orderInfoForm, orderInfo);
        //订单号和订单状态需要自己设置
        orderInfo.setOrderNo(UUID.randomUUID().toString().replace("-", ""));
        //枚举类的等待接单
        orderInfo.setStatus(OrderStatus.WAITING_ACCEPT.getStatus());
        //记录日志
        this.log(orderInfo.getId(),orderInfo.getStatus());
        orderInfoMapper.insert(orderInfo);

        //向redis添加标识
        //接单标识，标识不存在了说明不在等待接单状态了
        redisTemplate.opsForValue().set(RedisConstant.ORDER_ACCEPT_MARK,
                "0", RedisConstant.ORDER_ACCEPT_MARK_EXPIRES_TIME, TimeUnit.MINUTES);

        //返回订单id
        return orderInfo.getId();
    }

    @Override
    public Integer getOrderStatus(Long orderId) {
        //select status from order_info where id = ?
        //用lambda指定对应的哪个类对应的表项名(实体类已经对应表名字了)更好
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getId,orderId);
        wrapper.select(OrderInfo::getStatus);
        OrderInfo orderInfo = orderInfoMapper.selectOne(wrapper);
        //订单不存在
        if(orderInfo == null) {
            return OrderStatus.NULL_ORDER.getStatus();
        }
        return orderInfo.getStatus();
    }

    //Redisson分布式锁
    //司机抢单
    @Override
    public Boolean robNewOrder(Long driverId, Long orderId) {
        System.out.println("抢到的单子orderId"+orderId);
        //判断订单是否存在，通过Redis，减少数据库压力
        if(!redisTemplate.hasKey(RedisConstant.ORDER_ACCEPT_MARK)) {
            //抢单失败
            throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
        }

        //创建锁
        RLock lock = redissonClient.getLock(RedisConstant.ROB_NEW_ORDER_LOCK + orderId);

        try {
            //获取锁
            boolean flag = lock.tryLock(RedisConstant.ROB_NEW_ORDER_LOCK_WAIT_TIME,RedisConstant.ROB_NEW_ORDER_LOCK_LEASE_TIME, TimeUnit.SECONDS);
            if(flag) {
                if(!redisTemplate.hasKey(RedisConstant.ORDER_ACCEPT_MARK)) {
                    //抢单失败
                    throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
                }
                //司机抢单
                //修改order_info表订单状态值2：已经接单 + 司机id + 司机接单时间
                //修改条件：根据订单id
                LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(OrderInfo::getId,orderId);
                OrderInfo orderInfo = orderInfoMapper.selectOne(wrapper);
                //设置
                System.out.println("设置的driverId"+driverId);
                orderInfo.setStatus(OrderStatus.ACCEPTED.getStatus());
                orderInfo.setDriverId(driverId);
                orderInfo.setAcceptTime(new Date());
                //调用方法修改
                int rows = orderInfoMapper.updateById(orderInfo);
                if(rows != 1) {
                    //抢单失败
                    throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
                }

                //删除抢单标识
                redisTemplate.delete(RedisConstant.ORDER_ACCEPT_MARK);
            }
        }catch (Exception e) {
            //抢单失败
            throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
        }finally {
            //释放
            if(lock.isLocked()) {
                lock.unlock();
            }
        }
        return true;
    }

    //乘客端查找当前订单
    @Override
    public CurrentOrderInfoVo searchCustomerCurrentOrder(Long customerId) {
        //封装条件
        //乘客id
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getCustomerId,customerId);
        //各种状态
        Integer[] statusArray = {
                OrderStatus.ACCEPTED.getStatus(),
                OrderStatus.DRIVER_ARRIVED.getStatus(),
                OrderStatus.UPDATE_CART_INFO.getStatus(),
                OrderStatus.START_SERVICE.getStatus(),
                OrderStatus.END_SERVICE.getStatus(),
                OrderStatus.UNPAID.getStatus()
        };
        //in！ 前面是对象 后面是自定义数组
        wrapper.in(OrderInfo::getStatus,statusArray);
        //orderByDesc对id进行降序排列
        wrapper.orderByDesc(OrderInfo::getId);
        //获取最新一条记录 limit 1 表示在sql最后加的东西：得到第一条数据
        wrapper.last(" limit 1");
        //调用方法 后端与数据库交互拿到的是全部数据 返回前端都是VO对象 表设计重要啊
        OrderInfo orderInfo = orderInfoMapper.selectOne(wrapper);
        //封装到CurrentOrderInfoVo
        CurrentOrderInfoVo currentOrderInfoVo = new CurrentOrderInfoVo();
        if(orderInfo != null) {
            currentOrderInfoVo.setOrderId(orderInfo.getId());
            currentOrderInfoVo.setStatus(orderInfo.getStatus());
            currentOrderInfoVo.setIsHasCurrentOrder(true);
        } else {
            currentOrderInfoVo.setIsHasCurrentOrder(false);
        }
        return currentOrderInfoVo;
    }

    //司机端查找当前订单
    @Override
    public CurrentOrderInfoVo searchDriverCurrentOrder(Long driverId) {
        //封装条件
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getDriverId,driverId);
        Integer[] statusArray = {
                OrderStatus.ACCEPTED.getStatus(),
                OrderStatus.DRIVER_ARRIVED.getStatus(),
                OrderStatus.UPDATE_CART_INFO.getStatus(),
                OrderStatus.START_SERVICE.getStatus(),
                OrderStatus.END_SERVICE.getStatus()
        };
        wrapper.in(OrderInfo::getStatus,statusArray);
        wrapper.orderByDesc(OrderInfo::getId);
        wrapper.last(" limit 1");
        OrderInfo orderInfo = orderInfoMapper.selectOne(wrapper);
        //封装到vo
        CurrentOrderInfoVo currentOrderInfoVo = new CurrentOrderInfoVo();
        if(null != orderInfo) {
            currentOrderInfoVo.setStatus(orderInfo.getStatus());
            currentOrderInfoVo.setOrderId(orderInfo.getId());
            currentOrderInfoVo.setIsHasCurrentOrder(true);
        } else {
            currentOrderInfoVo.setIsHasCurrentOrder(false);
        }
        return currentOrderInfoVo;
    }

    //司机到达起始点
    @Override
    public Boolean driverArriveStartLocation(Long orderId, Long driverId) {
        // 更新订单状态和到达时间，条件：orderId + driverId
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getId,orderId);
        wrapper.eq(OrderInfo::getDriverId,driverId);

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setStatus(OrderStatus.DRIVER_ARRIVED.getStatus());
        orderInfo.setArriveTime(new Date());

        int rows = orderInfoMapper.update(orderInfo, wrapper);

        if(rows == 1) {
            return true;
        } else {
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }
    }

    @Override
    public Boolean updateOrderCart(UpdateOrderCartForm updateOrderCartForm) {
        System.out.println("前端传过来的UpdateOrderCartForm"+updateOrderCartForm.getCarFrontUrl());
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getId,updateOrderCartForm.getOrderId());
        wrapper.eq(OrderInfo::getDriverId,updateOrderCartForm.getDriverId());
        OrderInfo orderInfo = new OrderInfo();
        BeanUtils.copyProperties(updateOrderCartForm,orderInfo);
        orderInfo.setStatus(OrderStatus.UPDATE_CART_INFO.getStatus());
        System.out.println("封装后的url"+orderInfo.getCarFrontUrl());

        int rows = orderInfoMapper.update(orderInfo, wrapper);
        if(rows == 1) {
            return true;
        } else {
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }
    }

    //开始代驾服务
    @Override
    public Boolean startDriver(StartDriveForm startDriveForm) {
        //根据订单id  +  司机id  更新订单状态  和 开始代驾时间
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getId,startDriveForm.getOrderId());
        wrapper.eq(OrderInfo::getDriverId,startDriveForm.getDriverId());

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setStatus(OrderStatus.START_SERVICE.getStatus());
        orderInfo.setStartServiceTime(new Date());

        int rows = orderInfoMapper.update(orderInfo, wrapper);
        if(rows == 1) {
            return true;
        } else {
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }
    }

    @Override
    public Long getOrderNumByTime(String startTime, String endTime) {
        // 09 <= time < 10   <= time1  <    11
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(OrderInfo::getStartServiceTime,startTime);
        wrapper.lt(OrderInfo::getStartServiceTime,endTime);
        Long count = orderInfoMapper.selectCount(wrapper);
        return count;
    }

    @Override
    public Boolean endDrive(UpdateOrderBillForm updateOrderBillForm) {
        //1 更新订单信息
        // update order_info set ..... where id=? and driver_id=?
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getId,updateOrderBillForm.getOrderId());
        wrapper.eq(OrderInfo::getDriverId,updateOrderBillForm.getDriverId());

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setStatus(OrderStatus.END_SERVICE.getStatus());
        orderInfo.setRealAmount(updateOrderBillForm.getTotalAmount());
        orderInfo.setFavourFee(updateOrderBillForm.getFavourFee());
        orderInfo.setRealDistance(updateOrderBillForm.getRealDistance());
        orderInfo.setEndServiceTime(new Date());

        int rows = orderInfoMapper.update(orderInfo, wrapper);

        if(rows == 1) {
            //添加账单数据
            OrderBill orderBill = new OrderBill();
            BeanUtils.copyProperties(updateOrderBillForm,orderBill);
            orderBill.setOrderId(updateOrderBillForm.getOrderId());
            orderBill.setPayAmount(updateOrderBillForm.getTotalAmount());
            orderBillMapper.insert(orderBill);

            //添加分账信息
            OrderProfitsharing orderProfitsharing = new OrderProfitsharing();
            BeanUtils.copyProperties(updateOrderBillForm, orderProfitsharing);
            orderProfitsharing.setOrderId(updateOrderBillForm.getOrderId());
            //TODO
            orderProfitsharing.setRuleId(new Date().getTime());
            orderProfitsharing.setStatus(1);
            orderProfitsharingMapper.insert(orderProfitsharing);

        } else {
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }
        return true;
    }

    //获取乘客订单分页列表
    @Override
    public PageVo findCustomerOrderPage(Page<OrderInfo> pageParam, Long customerId) {
        IPage<OrderListVo> pageInfo =  orderInfoMapper.selectCustomerOrderPage(pageParam,customerId);
        return new PageVo<>(pageInfo.getRecords(),pageInfo.getPages(),pageInfo.getTotal());
    }

    @Override
    public PageVo findDriverOrderPage(Page<OrderInfo> pageParam, Long driverId) {
        IPage<OrderListVo> pageInfo =  orderInfoMapper.selectDriverOrderPage(pageParam,driverId);
        return new PageVo<>(pageInfo.getRecords(),pageInfo.getPages(),pageInfo.getTotal());
    }

    @Override
    public OrderBillVo getOrderBillInfo(Long orderId) {
        LambdaQueryWrapper<OrderBill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderBill::getOrderId,orderId);
        OrderBill orderBill = orderBillMapper.selectOne(wrapper);

        OrderBillVo orderBillVo = new OrderBillVo();
        BeanUtils.copyProperties(orderBill,orderBillVo);
        return orderBillVo;
    }

    @Override
    public OrderProfitsharingVo getOrderProfitsharing(Long orderId) {
        LambdaQueryWrapper<OrderProfitsharing> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderProfitsharing::getOrderId,orderId);
        OrderProfitsharing orderProfitsharing = orderProfitsharingMapper.selectOne(wrapper);

        OrderProfitsharingVo orderProfitsharingVo = new OrderProfitsharingVo();
        BeanUtils.copyProperties(orderProfitsharing,orderProfitsharingVo);
        return orderProfitsharingVo;
    }

    @Override
    public Boolean sendOrderBillInfo(Long orderId, Long driverId) {
        //更新订单信息
        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderInfo::getId, orderId);
        queryWrapper.eq(OrderInfo::getDriverId, driverId);
        //更新字段
        OrderInfo updateOrderInfo = new OrderInfo();
        updateOrderInfo.setStatus(OrderStatus.UNPAID.getStatus());
        //只能更新自己的订单
        int row = orderInfoMapper.update(updateOrderInfo, queryWrapper);
        if(row == 1) {
            return true;
        } else {
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }
    }

    @Override
    public OrderPayVo getOrderPayVo(String orderNo, Long customerId) {
        OrderPayVo orderPayVo = orderInfoMapper.selectOrderPayVo(orderNo,customerId);
        if(orderPayVo != null) {
            String content = orderPayVo.getStartLocation() + " 到 "+orderPayVo.getEndLocation();
            orderPayVo.setContent(content);
        }
        return orderPayVo;
    }

    @Override
    public BigDecimal getRealDistance(Long driverId) {
        LambdaQueryWrapper<OrderInfo> orderInfoLambdaQueryWrapper = new LambdaQueryWrapper<>();
        orderInfoLambdaQueryWrapper.eq(OrderInfo::getDriverId, driverId);
        orderInfoLambdaQueryWrapper.eq(OrderInfo::getStatus, 5);
        OrderInfo orderInfo1 = orderInfoMapper.selectOne(orderInfoLambdaQueryWrapper);
        System.out.println("orderInfo1:" + orderInfo1);
        return orderInfo1.getExpectDistance();
    }

    public Boolean robNewOrder2(Long driverId, Long orderId) {
        //判断订单是否存在，通过redisTemplate 如果不存在这个key就返回抢单失败
        if (!redisTemplate.hasKey(RedisConstant.ORDER_ACCEPT_MARK)) {
            throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
        }
        //司机抢单
        //修改order_info表订单状态2：已经接单 + 司机id +接单时间
        //修改条件，根据订单id得到数据
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getId,orderId);
        OrderInfo orderInfo = orderInfoMapper.selectOne(wrapper);
        orderInfo.setDriverId(driverId);
        orderInfo.setStatus(2);
        orderInfo.setAcceptTime(new Date());
        int rows = orderInfoMapper.updateById(orderInfo);
        if(rows != 1) {
            //抢单失败
            throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
        }
        //删除抢单的标识
        redisTemplate.delete(RedisConstant.ORDER_ACCEPT_MARK);
        return true;
    }

    //司机抢单：乐观锁方案解决并发问题
    public Boolean robNewOrder1(Long driverId, Long orderId) {
        //判断订单是否存在，通过Redis，减少数据库压力
        if(!redisTemplate.hasKey(RedisConstant.ORDER_ACCEPT_MARK)) {
            //抢单失败
            throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
        }

        //司机抢单
        //update order_info set status =2 ,driver_id = ?,accept_time = ?
        // where id=? and status = 1
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getId,orderId);
        wrapper.eq(OrderInfo::getStatus,OrderStatus.WAITING_ACCEPT.getStatus());

        //修改值
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setStatus(OrderStatus.ACCEPTED.getStatus());
        orderInfo.setDriverId(driverId);
        orderInfo.setAcceptTime(new Date());

        //调用方法修改
        int rows = orderInfoMapper.update(orderInfo,wrapper);
        if(rows != 1) {
            //抢单失败
            throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
        }

        //删除抢单标识
        redisTemplate.delete(RedisConstant.ORDER_ACCEPT_MARK);
        return true;
    }

    private void log(Long orderId , Integer status){
        OrderStatusLog orderStatusLog = new OrderStatusLog();
        orderStatusLog.setOrderId(orderId);
        orderStatusLog.setOrderStatus(status);
        orderStatusLog.setOperateTime(new Date());
        orderStatusLogMapper.insert(orderStatusLog);
    }
}
