package com.atguigu.daijia.dispatch.xxl.job;

import com.atguigu.daijia.dispatch.mapper.XxlJobLogMapper;
import com.atguigu.daijia.dispatch.service.NewOrderService;
import com.atguigu.daijia.model.entity.dispatch.XxlJobLog;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class JobHandler {
    @Resource
    private XxlJobLogMapper xxlJobLogMapper;
    @Resource
    private NewOrderService newOrderService;
    //要和XxlJobClient的executorHandler一致
    @XxlJob("newOrderTaskHandler")
    public void newOrderTaskHandler() {
        XxlJobLog log = new XxlJobLog();
        log.setJobId(XxlJobHelper.getJobId());
        //记录程序运行时间        log.setTimes();
        long startTime = System.currentTimeMillis();

        //记录任务调度日志
        try{
            //执行任务 搜索附近代驾司机
            newOrderService.executeTask(XxlJobHelper.getJobId());


            //成功状态
            log.setStatus(1);
        }catch (Exception e){
            //失败状态
            log.setStatus(0);
            e.printStackTrace();
        }finally {
            long endTime = System.currentTimeMillis();
            log.setTimes(endTime-startTime);
            xxlJobLogMapper.insert(log);
        }
    }
}
