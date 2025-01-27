package com.atguigu.daijia.customer.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import com.atguigu.daijia.customer.mapper.CustomerInfoMapper;
import com.atguigu.daijia.customer.mapper.CustomerLoginLogMapper;
import com.atguigu.daijia.customer.service.CustomerInfoService;
import com.atguigu.daijia.model.entity.customer.CustomerInfo;
import com.atguigu.daijia.model.entity.customer.CustomerLoginLog;
import com.atguigu.daijia.model.vo.customer.CustomerLoginVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.netty.util.internal.StringUtil;
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
public class CustomerInfoServiceImpl extends ServiceImpl<CustomerInfoMapper, CustomerInfo> implements CustomerInfoService {
    @Resource
    private WxMaService wxMaService;
    //注入当前层的mapper
    @Resource
    private CustomerInfoMapper infoMapper;
    @Resource
    private CustomerLoginLogMapper customerLoginLogMapper;
    @Override
    public Long login(String code) throws WxErrorException {
        String openid = null;
        //1.获取code值 使用微信工具包对象(WxMaService) 获取微信唯一标识 openid
        WxMaJscode2SessionResult sessionInfo = wxMaService.getUserService().getSessionInfo(code);
        openid = sessionInfo.getOpenid();
        //2.根据openid判断是否第一次登录 是-》添加信息到用户表 返回用户id值 plus登录日志
//        LambdaQueryWrapper<CustomerInfo> queryWrapper = new LambdaQueryWrapper<>();
        QueryWrapper<CustomerInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("wx_open_id", openid);
//        queryWrapper.eq(CustomerInfo::getWxOpenId,openid);
        CustomerInfo customerInfo = infoMapper.selectOne(queryWrapper);
        if (customerInfo == null) {
            //new CustomerInfo() 是必需的 - 因为 customerInfo 为 null 时我们需要一个新对象来存储数据。
            // 否则无法调用 setWxOpenId() 等方法，会报 NullPointerException。
            customerInfo = new CustomerInfo();
            customerInfo.setWxOpenId(openid);
            String phone = String.format("%11d", new Random().nextInt(10000));
            customerInfo.setPhone(phone);
            customerInfo.setAvatarUrl("https://oss.aliyuncs.com/aliyun_id_photo_bucket/default_handsome.jpg");
            customerInfo.setNickname("newGuy"+"phone");
            infoMapper.insert(customerInfo);
        }
        CustomerLoginLog customerLoginLog = new CustomerLoginLog();
        customerLoginLog.setCustomerId(customerInfo.getId());
        customerLoginLog.setMsg("小程序登录");
        customerLoginLogMapper.insert(customerLoginLog);
        //3.返回用户id
        return customerInfo.getId();
    }

    @Override
    public CustomerLoginVo getCustomerInfo(Long customerId) {
        //根据id查询客户信息
        CustomerInfo customerInfo = infoMapper.selectById(customerId);
        //封装到VO
        CustomerLoginVo customerLoginVo = new CustomerLoginVo();
        BeanUtils.copyProperties(customerInfo, customerLoginVo);
        //看是否有手机号 VO里特有的一个值 是否绑定手机号
        String phone = customerInfo.getPhone();
        boolean b = StringUtils.hasText(phone);
        customerLoginVo.setIsBindPhone(b);
        //返回
        return customerLoginVo;
    }
}
