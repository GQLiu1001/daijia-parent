package com.atguigu.daijia.common.login;

import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.common.util.AuthContextHolder;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component //注册进SpringBoot
@Aspect //表示这是个切面类
public class GuiguLoginAspect {
    @Resource
    private RedisTemplate redisTemplate;
    @Around("execution(* com.atguigu.daijia.*.controller.*.*(..)) && @annotation(guiguLogin)")//环绕通知 进行登录判断 里面内容为切入点表达式 选择哪些内容进行增强
    //第一个* public private...  第二个包和匹配com.atguigu.daijia.*.controller.*.*(..))
    //除了controller还得是有注解才能切入
    public Object login(ProceedingJoinPoint joinPoint, GuiguLogin guiguLogin) throws Throwable {
        //获取request对象 Spring提供的工具类RequestAttributes ServletRequestAttributes .getRequest()
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        ServletRequestAttributes sra = (ServletRequestAttributes) requestAttributes;
        HttpServletRequest request = sra.getRequest();
        //从请求头获取token
        String token = request.getHeader("token");
        //token空 需要登录
        if (token == null) {
            throw new GuiguException(ResultCodeEnum.LOGIN_AUTH);
        }
        String id = (String) redisTemplate.opsForValue().get(token);
        //查询redis对应用户id 存入ThreadLocal(和当前线程绑定的对象)
        if (id != null) {
            //AuthContextHolder ThreadLocal的工具类
            AuthContextHolder.setUserId(Long.valueOf(id));
        }
        System.out.println("切面类");
        //执行业务方法
        return joinPoint.proceed();
    }
}
