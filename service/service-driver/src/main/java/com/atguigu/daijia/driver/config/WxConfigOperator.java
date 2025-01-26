package com.atguigu.daijia.driver.config;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.impl.WxMaServiceImpl;
import cn.binarywang.wx.miniapp.config.impl.WxMaDefaultConfigImpl;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
//微信小程序配置类
@Component
public class WxConfigOperator {
    @Resource
    private WxConfigProperties wxConfigProperties;
    @Bean
    public WxMaService wxMaService() {
        WxMaService service = new WxMaServiceImpl();
        WxMaDefaultConfigImpl config =new WxMaDefaultConfigImpl();
        config.setAppid(wxConfigProperties.getAppId());
        config.setSecret(wxConfigProperties.getSecret());
        service.setWxMaConfig(config);
        return service;
    }
}
