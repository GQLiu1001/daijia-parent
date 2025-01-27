package com.atguigu.daijia.customer.controller;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.customer.service.CustomerInfoService;
import com.atguigu.daijia.model.entity.customer.CustomerInfo;
import com.atguigu.daijia.model.entity.customer.CustomerLoginLog;
import com.atguigu.daijia.model.vo.customer.CustomerInfoVo;
import com.atguigu.daijia.model.vo.customer.CustomerLoginVo;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/customer/info")
@SuppressWarnings({"unchecked", "rawtypes"})
public class CustomerInfoController {
	//注入当前层的Service
	@Resource
	private CustomerInfoService infoService;

	@Operation(summary = "授权登录")
	@GetMapping("/login/{code}")
	public Result<Long> login(@PathVariable("code") String code) throws WxErrorException {
		return Result.ok(infoService.login(code));
	}

	//用VO封装
	@Operation(summary = "获取客户基本信息")
	@GetMapping("/getCustomerLoginInfo/{customerId}")
	public Result<CustomerLoginVo> getCustomerInfo(@PathVariable Long customerId) {
		CustomerLoginVo customerLoginVo = infoService.getCustomerInfo(customerId);
		return Result.ok(customerLoginVo);
	}
}

