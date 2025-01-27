package com.atguigu.daijia.driver.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import com.atguigu.daijia.driver.mapper.DriverInfoMapper;
import com.atguigu.daijia.driver.mapper.DriverLoginLogMapper;
import com.atguigu.daijia.driver.service.DriverInfoService;
import com.atguigu.daijia.model.entity.customer.CustomerInfo;
import com.atguigu.daijia.model.entity.driver.DriverInfo;
import com.atguigu.daijia.model.entity.driver.DriverLoginLog;
import com.atguigu.daijia.model.vo.customer.CustomerLoginVo;
import com.atguigu.daijia.model.vo.driver.DriverLoginVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Random;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class DriverInfoServiceImpl extends ServiceImpl<DriverInfoMapper, DriverInfo> implements DriverInfoService {
    @Resource
    private WxMaService wxMaService;
    @Resource
    private DriverLoginLogMapper driverLoginLogMapper;
    @Resource
    private DriverInfoMapper driverInfoMapper;
    @Override
    public Long login(String code) throws WxErrorException {
        String openid = null;
        //1.获取code值 使用微信工具包对象(WxMaService) 获取微信唯一标识 openid
        WxMaJscode2SessionResult sessionInfo = wxMaService.getUserService().getSessionInfo(code);
        openid = sessionInfo.getOpenid();
        //2.根据openid判断是否第一次登录 是-》添加信息到用户表 返回用户id值 plus登录日志
//        LambdaQueryWrapper<CustomerInfo> queryWrapper = new LambdaQueryWrapper<>();
        QueryWrapper<DriverInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("wx_open_id", openid);
//        queryWrapper.eq(CustomerInfo::getWxOpenId,openid);
        DriverInfo driverInfo = driverInfoMapper.selectOne(queryWrapper);
        if (driverInfo == null) {
            //new driverInfo() 是必需的 - 因为 driverInfo 为 null 时我们需要一个新对象来存储数据。
            // 否则无法调用 setWxOpenId() 等方法，会报 NullPointerException。
            driverInfo = new DriverInfo();
            driverInfo.setWxOpenId(openid);
            String phone = String.format("%11d", new Random().nextInt(10000));
            driverInfo.setPhone(phone);
            driverInfo.setAvatarUrl("https://oss.aliyuncs.com/aliyun_id_photo_bucket/default_handsome.jpg");
            driverInfo.setNickname("newDriver"+"phone");
            driverInfoMapper.insert(driverInfo);
        }
        DriverLoginLog driverLoginLog = new DriverLoginLog();
        driverLoginLog.setDriverId(driverInfo.getId());
        driverLoginLog.setMsg("小程序登录");
        driverLoginLogMapper.insert(driverLoginLog);
        //3.返回用户id
        return driverInfo.getId();
    }

    @Override
    public DriverLoginVo getDriverInfo(Long driverId) {
        //根据id查询客户信息
        DriverInfo driverInfo = driverInfoMapper.selectById(driverId);
        //封装到VO
        DriverLoginVo driverLoginVo = new DriverLoginVo();
        BeanUtils.copyProperties(driverInfo, driverLoginVo);
        return driverLoginVo;
    }
}