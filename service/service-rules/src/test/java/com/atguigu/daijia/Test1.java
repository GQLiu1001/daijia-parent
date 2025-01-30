package com.atguigu.daijia;

import com.alibaba.fastjson2.JSON;
import com.atguigu.daijia.config.DroolsConfig;
import com.atguigu.daijia.model.form.rules.FeeRuleRequest;
import com.atguigu.daijia.model.vo.rules.FeeRuleResponse;
import org.junit.jupiter.api.Test;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

@SpringBootTest(classes = {DroolsConfig.class})
public class Test1 {
    @Autowired
    private KieContainer kieContainer;
    @Test
    void test1() {
        FeeRuleRequest feeRuleRequest = new FeeRuleRequest();
        feeRuleRequest.setDistance(new BigDecimal(15.0));
        feeRuleRequest.setStartTime("01:59:59");
        feeRuleRequest.setWaitMinute(20);

        // 开启会话
        KieSession kieSession = kieContainer.newKieSession();

        FeeRuleResponse feeRuleResponse = new FeeRuleResponse();
        kieSession.setGlobal("feeRuleResponse", feeRuleResponse);
        // 设置订单对象
        kieSession.insert(feeRuleRequest);
        // 触发规则
        kieSession.fireAllRules();
        // 中止会话
        kieSession.dispose();
        System.out.println("后："+ JSON.toJSONString(feeRuleResponse));
    }
}
