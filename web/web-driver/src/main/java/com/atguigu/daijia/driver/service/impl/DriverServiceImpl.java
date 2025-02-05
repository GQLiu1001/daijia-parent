package com.atguigu.daijia.driver.service.impl;

import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.common.util.AuthContextHolder;
import com.atguigu.daijia.customer.client.CustomerInfoFeignClient;
import com.atguigu.daijia.dispatch.client.NewOrderFeignClient;
import com.atguigu.daijia.driver.client.DriverInfoFeignClient;
import com.atguigu.daijia.driver.service.DriverService;
import com.atguigu.daijia.map.client.LocationFeignClient;
import com.atguigu.daijia.model.form.driver.DriverFaceModelForm;
import com.atguigu.daijia.model.form.driver.UpdateDriverAuthInfoForm;
import com.atguigu.daijia.model.vo.driver.DriverAuthInfoVo;
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
    @Resource
    private LocationFeignClient locationClient;
    @Resource
    private NewOrderFeignClient newOrderClient;

    @Override
    public String login(String code) {
        //1.拿着code进行远程调用 返回用户id
        Result<Long> login = client.login(code);
        //2.返回失败，返回错误提示
        if (login.getCode() != 200) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        //3.获取返回id
        Long id = login.getData();
        //3.id为空 错误提示
        if (id == null) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        //4.token字符串
        String token = UUID.randomUUID().toString().replaceAll("-", "");
        //5.token放入redis 设置时限
        redisTemplate.opsForValue().set(token, id.toString(), 1, TimeUnit.DAYS);
        //6.返回token
        return token;
    }

    @Override
    public DriverLoginVo getInfo(String token) {
        String o = (String) redisTemplate.opsForValue().get(token);
        Result<DriverLoginVo> driverInfo = client.getDriverInfo(Long.valueOf(o));
        if (driverInfo.getCode() != 200) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        if (driverInfo.getData() == null) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        return driverInfo.getData();
    }

//    @Override
//    public DriverAuthInfoVo getDriverAuthInfo(Long id) {
//        Result<DriverAuthInfoVo> driverAuthInfo = client.getDriverAuthInfo(id);
//        return driverAuthInfo.getData();
//    }
    //司机认证信息
    @Override
    public DriverAuthInfoVo getDriverAuthInfo(Long driverId) {
        Result<DriverAuthInfoVo> authInfoVoResult = client.getDriverAuthInfo(driverId);
        return authInfoVoResult.getData();
    }

    @Override
    public Boolean updateDriverAuthInfo(UpdateDriverAuthInfoForm form) {
        Result<Boolean> booleanResult = client.updateDriverAuthInfo(form);
        return booleanResult.getData();
    }

    @Override
    public Boolean creatDriverFaceModel(DriverFaceModelForm form) {
        Result<Boolean> booleanResult = client.creatDriverFaceModel(form);
        return booleanResult.getData();
    }

    @Override
    public Boolean isFaceRecognition(Long driverId) {
        return client.isFaceRecognition(driverId).getData();
    }

    @Override
    public Boolean verifyDriverFace(DriverFaceModelForm driverFaceModelForm) {
        Result<Boolean> booleanResult = client.verifyDriverFace(driverFaceModelForm);
        return booleanResult.getData();
    }

    //开始接单服务
    @Override
    public Boolean startService(Long driverId) {
        //1 判断完成认证
        DriverLoginVo driverLoginVo = client.getDriverInfo(driverId).getData();
        if(driverLoginVo.getAuthStatus()!=2) {
            throw new GuiguException(ResultCodeEnum.AUTH_ERROR);
        }

        //2 判断当日是否人脸识别
        Boolean isFace = client.isFaceRecognition(driverId).getData();
        if(!isFace) {
            throw new GuiguException(ResultCodeEnum.FACE_ERROR);
        }

        //3 更新订单状态 1 开始接单
        client.updateServiceStatus(driverId,1);

        //4 删除redis司机位置信息 LocationFeignClient
        locationClient.removeDriverLocation(driverId);

        //5 清空司机临时队列数据 NewOrderFeignClient
        newOrderClient.clearNewOrderQueueData(driverId);
        return true;
    }

    //停止接单服务
    @Override
    public Boolean stopService(Long driverId) {
        //更新司机的接单状态 0
        client.updateServiceStatus(driverId,0);
        //删除司机位置信息
        locationClient.removeDriverLocation(driverId);
        //清空司机临时队列
        newOrderClient.clearNewOrderQueueData(driverId);
        return true;
    }
}
