package com.atguigu.daijia.driver.service;

import com.atguigu.daijia.model.entity.driver.DriverInfo;
import com.atguigu.daijia.model.vo.customer.CustomerLoginVo;
import com.atguigu.daijia.model.vo.driver.DriverLoginVo;
import com.baomidou.mybatisplus.extension.service.IService;
import me.chanjar.weixin.common.error.WxErrorException;

public interface DriverInfoService extends IService<DriverInfo> {

    Long login(String code) throws WxErrorException;

    DriverLoginVo getDriverInfo(Long customerId);
}
