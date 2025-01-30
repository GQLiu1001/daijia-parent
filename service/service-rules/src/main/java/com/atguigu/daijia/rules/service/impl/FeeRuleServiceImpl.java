package com.atguigu.daijia.rules.service.impl;

import com.atguigu.daijia.model.form.rules.FeeRuleRequest;
import com.atguigu.daijia.model.form.rules.FeeRuleRequestForm;
import com.atguigu.daijia.model.vo.rules.FeeRuleResponse;
import com.atguigu.daijia.model.vo.rules.FeeRuleResponseVo;
import com.atguigu.daijia.rules.mapper.FeeRuleMapper;
import com.atguigu.daijia.rules.service.FeeRuleService;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class FeeRuleServiceImpl implements FeeRuleService {
    @Resource
    private KieContainer kieContainer;
    @Override
    public FeeRuleResponseVo calculateOrderFee(FeeRuleRequestForm calculateOrderFeeForm) {
        //输入form -> 输入对象 -> drl.insert.fireAllRules.dispose -> 输出对象 -> 输出vo
        //封装输入对象
        FeeRuleRequest request = new FeeRuleRequest();
        request.setDistance(calculateOrderFeeForm.getDistance());
        Date startDate = calculateOrderFeeForm.getStartTime();
        request.setStartTime(new DateTime(startDate).toString("HH:mm:ss"));
        request.setWaitMinute(calculateOrderFeeForm.getWaitMinute());
        //Drools使用
        KieSession kieSession = kieContainer.newKieSession();
        //封装返回对象
        FeeRuleResponse response = new FeeRuleResponse();
        //设置全局变量，在drl规则里可以设置进去
        kieSession.setGlobal("feeRuleResponse", response);
        kieSession.insert(request);
        kieSession.fireAllRules();
        kieSession.dispose();
        //封装VO返回
        FeeRuleResponseVo responseVo = new FeeRuleResponseVo();
        BeanUtils.copyProperties(response, responseVo);
        return responseVo;
    }
}
