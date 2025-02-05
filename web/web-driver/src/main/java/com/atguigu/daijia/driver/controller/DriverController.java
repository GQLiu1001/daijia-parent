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
import com.atguigu.daijia.model.vo.order.CurrentOrderInfoVo;
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
@RequestMapping(value = "/driver")
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
        System.out.println("触发了获取司机登录信息");
        DriverLoginVo driverLoginInfo = driverService.getInfo(token);
        //调用service 返回VO对象
        return Result.ok(driverLoginInfo);
    }

    @Operation(summary = "获取司机认证信息")
    @GuiguLogin
    @GetMapping("/getDriverAuthInfo")
    public Result<DriverAuthInfoVo> getDriverAuthInfo() {
        System.out.println("触发了获取司机认证信息");
        //获取登录用户id，当前是司机id
        Long driverId = AuthContextHolder.getUserId();
        return Result.ok(driverService.getDriverAuthInfo(driverId));
    }
//    @GuiguLogin
//    @Operation(summary = "获取司机认证信息")
//    @GetMapping("/getDriverAuthInfo/{driverId}")
//    public Result<DriverAuthInfoVo> getDriverAuthInfo(@PathVariable("driverId") Long id) {
//        DriverAuthInfoVo vo = driverService.getDriverAuthInfo(id);
//        return Result.ok(vo);
//    }

    @GuiguLogin
    @Operation(summary = "更新司机认证信息")
    @PostMapping("/updateDriverAuthInfo")
    public Result<Boolean> updateDriverAuthInfo(@RequestBody UpdateDriverAuthInfoForm form) {
        System.out.println("触发了更新司机认证信息");
        //因为ThreadLocal只能单服务调用所以先在web服务里取出id
        form.setDriverId(AuthContextHolder.getUserId());
        return Result.ok(driverService.updateDriverAuthInfo(form));
    }

    @GuiguLogin
    @Operation(summary = "创建司机人脸模型")
    @PostMapping("/creatDriverFaceModel")
    public Result<Boolean> creatDriverFaceModel(@RequestBody DriverFaceModelForm form) {
        System.out.println("触发了创建司机人脸模型");
        form.setDriverId(AuthContextHolder.getUserId());
        //都传的Base64
        return Result.ok(driverService.creatDriverFaceModel(form));
    }

    @Operation(summary = "查找司机端当前订单")
    @GuiguLogin
    @GetMapping("/searchDriverCurrentOrder")
    public Result<CurrentOrderInfoVo> searchDriverCurrentOrder() {
        CurrentOrderInfoVo currentOrderInfoVo = new CurrentOrderInfoVo();
        currentOrderInfoVo.setIsHasCurrentOrder(false);
        return Result.ok(currentOrderInfoVo);
    }

    @Operation(summary = "判断司机当日是否进行过人脸识别")
    @GuiguLogin
    @GetMapping("/isFaceRecognition")
    Result<Boolean> isFaceRecognition() {
        System.out.println("触发了判断司机当日是否进行过人脸识别");
        Long driverId = AuthContextHolder.getUserId();
        return Result.ok(driverService.isFaceRecognition(driverId));
    }

    @Operation(summary = "验证司机人脸")
    @GuiguLogin
    @PostMapping("/verifyDriverFace")
    public Result<Boolean> verifyDriverFace(@RequestBody DriverFaceModelForm driverFaceModelForm) {
        System.out.println("触发了验证司机人脸");
        driverFaceModelForm.setDriverId(AuthContextHolder.getUserId());
        return Result.ok(driverService.verifyDriverFace(driverFaceModelForm));
    }

    @Operation(summary = "开始接单服务")
    @GuiguLogin
    @GetMapping("/startService")
    public Result<Boolean> startService() {
        Long driverId = AuthContextHolder.getUserId();
        return Result.ok(driverService.startService(driverId));
    }

    @Operation(summary = "停止接单服务")
    @GuiguLogin
    @GetMapping("/stopService")
    public Result<Boolean> stopService() {
        Long driverId = AuthContextHolder.getUserId();
        return Result.ok(driverService.stopService(driverId));
    }


}

