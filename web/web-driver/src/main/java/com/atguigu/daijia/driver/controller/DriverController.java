package com.atguigu.daijia.driver.controller;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.util.AuthContextHolder;
import com.atguigu.daijia.driver.service.DriverService;
import com.atguigu.daijia.model.vo.customer.CustomerLoginVo;
import com.atguigu.daijia.model.vo.driver.DriverLoginVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "司机API接口管理")
@RestController
@RequestMapping(value="/driver")
@SuppressWarnings({"unchecked", "rawtypes"})
public class DriverController {
    @Resource
    private DriverService driverService;
    @Operation(summary = "授权登录")
    @GetMapping("/login/{code}")
        public Result<String> login(@PathVariable("code") String code) {
        return Result.ok(driverService.login(code));
    }
    @Operation(summary = "获取司机登录信息")
    @GetMapping("/getDriverLoginInfo")
    public Result<DriverLoginVo> getDriverLoginVo(@RequestHeader("token") String token) {
        DriverLoginVo driverLoginInfo = driverService.getInfo(token);
        //调用service 返回VO对象
        return Result.ok(driverLoginInfo);
    }
}

