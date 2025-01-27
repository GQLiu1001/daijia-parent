package com.atguigu.daijia.driver.service.impl;

import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.customer.client.CustomerInfoFeignClient;
import com.atguigu.daijia.driver.client.DriverInfoFeignClient;
import com.atguigu.daijia.driver.service.DriverService;
import com.atguigu.daijia.model.vo.driver.DriverLoginVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class DriverServiceImpl implements DriverService {
    @Resource//注入远程调用接口
    private DriverInfoFeignClient client;
    @Resource
    private RedisTemplate redisTemplate;
    @Override
    public String login(String code) {
        //1.拿着code进行远程调用 返回用户id
        Result<Long> login = client.login(code);
        //2.返回失败，返回错误提示
        if (login.getCode() != 200) {
            throw  new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        //3.获取返回id
        Long id = login.getData();
        //3.id为空 错误提示
        if (id == null) {
            throw  new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        //4.token字符串
        String token = UUID.randomUUID().toString().replaceAll("-", "");
        //5.token放入redis 设置时限
        redisTemplate.opsForValue().set(token,id.toString(),1, TimeUnit.DAYS);
        //6.返回token
        return token;
    }

    @Override
    public DriverLoginVo getInfo(String token) {
        String o = (String) redisTemplate.opsForValue().get(token);
        Result<DriverLoginVo> driverInfo = client.getDriverInfo(Long.valueOf(o));
        if (driverInfo.getCode() != 200) {
            throw  new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        if (driverInfo.getData() == null) {
            throw  new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        return driverInfo.getData();
    }
}
