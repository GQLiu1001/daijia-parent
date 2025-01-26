package com.atguigu.daijia.customer.controller;

import com.atguigu.daijia.common.result.Result;

import com.atguigu.daijia.customer.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "客户API接口管理")
@RestController
@RequestMapping("/customer")
@SuppressWarnings({"unchecked", "rawtypes"})
public class CustomerController {
    @Resource
    private CustomerService customerService;

    @Operation(summary = "授权登录")
    @GetMapping("/login/{code}")
    public Result<String> login(@PathVariable("code") String code) {
        return Result.ok(customerService.login(code));
    }

}

