package com.atguigu.daijia.customer.controller;

import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.login.GuiguLogin;
import com.atguigu.daijia.common.result.Result;

import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.common.util.AuthContextHolder;
import com.atguigu.daijia.customer.client.CustomerInfoFeignClient;
import com.atguigu.daijia.customer.service.CustomerService;
import com.atguigu.daijia.model.vo.customer.CustomerLoginVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Tag(name = "客户API接口管理")
@RestController
@RequestMapping("/customer")
@SuppressWarnings({"unchecked", "rawtypes"})
public class CustomerController {
    @Resource
    private RedisTemplate redisTemplate;
    @Resource
    private CustomerService customerService;
    @Resource
    private CustomerInfoFeignClient customerInfoFeignClient;

    @Operation(summary = "授权登录")
    @GetMapping("/login/{code}")
    public Result<String> login(@PathVariable("code") String code) {
        return Result.ok(customerService.login(code));
    }

    //需要登陆判断的都要这个注解
    @GuiguLogin
    @Operation(summary = "获取客户登录信息")
    @GetMapping("/getCustomerLoginInfo")
    public Result<CustomerLoginVo> getCustomerLoginInfo(@RequestHeader("token") String token) {
        //从ThreadLocal获取id
        Long customId = AuthContextHolder.getUserId();
        CustomerLoginVo customerLoginInfo = customerService.getInfo(customId);
        //调用service 返回VO对象
//        CustomerLoginVo customerLoginInfo = customerService.getCustomerLoginInfo(token);
        return Result.ok(customerLoginInfo);
    }

}

