package com.atguigu.daijia.driver.service;

import com.atguigu.daijia.model.entity.driver.DriverInfo;
import com.atguigu.daijia.model.entity.driver.DriverSet;
import com.atguigu.daijia.model.form.driver.DriverFaceModelForm;
import com.atguigu.daijia.model.form.driver.UpdateDriverAuthInfoForm;
import com.atguigu.daijia.model.vo.customer.CustomerLoginVo;
import com.atguigu.daijia.model.vo.driver.DriverAuthInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverLoginVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import me.chanjar.weixin.common.error.WxErrorException;

public interface DriverInfoService extends IService<DriverInfo> {

    Long login(String code) throws WxErrorException;

    DriverLoginVo getDriverInfo(Long customerId);

    DriverAuthInfoVo getDriverAuthInfo(Long id);

    Boolean updateDriverAuthInfo(UpdateDriverAuthInfoForm form);

    Boolean creatDriverFaceModel(DriverFaceModelForm form) throws TencentCloudSDKException;

    DriverSet getDriverSet(Long driverId);

    Boolean isFaceRecognition(Long driverId);

    Boolean verifyDriverFace(DriverFaceModelForm driverFaceModelForm);

    Boolean updateServiceStatus(Long driverId, Integer status);

    DriverInfoVo getDriverInfoOrder(Long driverId);

    String getDriverOpenId(Long driverId);

    void increaseOrderCount(Long driverId);
}
