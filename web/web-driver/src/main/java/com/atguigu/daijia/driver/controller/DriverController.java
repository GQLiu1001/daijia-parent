package com.atguigu.daijia.driver.controller;

import com.atguigu.daijia.common.login.GuiguLogin;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.util.AuthContextHolder;
import com.atguigu.daijia.driver.service.DriverService;
import com.atguigu.daijia.model.form.driver.DriverFaceModelForm;
import com.atguigu.daijia.model.form.driver.UpdateDriverAuthInfoForm;
import com.atguigu.daijia.model.vo.customer.CustomerLoginVo;
import com.atguigu.daijia.model.vo.driver.DriverAuthInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverLoginVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    @GuiguLogin
    @Operation(summary = "获取司机登录信息")
    @GetMapping("/getDriverLoginInfo")
    public Result<DriverLoginVo> getDriverLoginVo(@RequestHeader("token") String token) {
        DriverLoginVo driverLoginInfo = driverService.getInfo(token);
        //调用service 返回VO对象
        return Result.ok(driverLoginInfo);
    }
    @GuiguLogin
    @Operation(summary = "获取司机认证信息")
    @GetMapping("/getDriverAuthInfo/{driverId}")
    public Result<DriverAuthInfoVo> getDriverAuthInfo(@PathVariable("driverId") Long id) {
        DriverAuthInfoVo vo = driverService.getDriverAuthInfo(id);
        return Result.ok(vo);
    }
    @GuiguLogin
    @Operation(summary = "更新司机认证信息")
    @PostMapping("/updateDriverAuthInfo")
    public Result<Boolean> updateDriverAuthInfo(@RequestBody UpdateDriverAuthInfoForm form) {
        form.setDriverId(AuthContextHolder.getUserId());
        return Result.ok(driverService.updateDriverAuthInfo(form));
    }
    @GuiguLogin
    @Operation(summary = "创建司机人脸模型")
    @PostMapping("/creatDriverFaceModel")
    public Result<Boolean>  creatDriverFaceModel(@RequestBody DriverFaceModelForm form) {
        form.setDriverId(AuthContextHolder.getUserId());
        //都传的Base64
        return Result.ok(driverService.creatDriverFaceModel(form));
    }
}

