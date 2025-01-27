package com.atguigu.daijia.driver.controller;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.driver.service.DriverInfoService;
import com.atguigu.daijia.model.vo.customer.CustomerLoginVo;
import com.atguigu.daijia.model.vo.driver.DriverLoginVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "司机API接口管理")
@RestController
@RequestMapping(value="/driver/info")
@SuppressWarnings({"unchecked", "rawtypes"})
public class DriverInfoController {
    @Resource
    private DriverInfoService driverInfoService;
    @Operation(summary = "授权登录")
    @GetMapping("/login/{code}")
    public Result<Long> login(@PathVariable("code") String code) throws WxErrorException {
        return Result.ok(driverInfoService.login(code));
    }
    //用VO封装
    @Operation(summary = "获取客户基本信息")
    @GetMapping("/getDriverLoginInfo/{customerId}")
    public Result<DriverLoginVo> getCustomerInfo(@PathVariable Long customerId) {
        DriverLoginVo driverLoginVo = driverInfoService.getDriverInfo(customerId);
        return Result.ok(driverLoginVo);
    }
}

