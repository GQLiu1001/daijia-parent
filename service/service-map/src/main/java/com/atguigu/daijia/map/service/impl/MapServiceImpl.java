package com.atguigu.daijia.map.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.map.service.MapService;
import com.atguigu.daijia.model.form.map.CalculateDrivingLineForm;
import com.atguigu.daijia.model.vo.map.DrivingLineVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class MapServiceImpl implements MapService {
    @Resource
    private RestTemplate restTemplate;

    //取配置文件的key
//    @Value("tencent.cloud.map")  蠢
    @Value("${tencent.map.key}")
    private String key;

    @Override
    public DrivingLineVo calculateDrivingLine(CalculateDrivingLineForm form) {
        //请求腾讯的接口，按照接口要求传参数，返回需要结果
        //用HTTP请求，用Spring的RestTemplate
        //定义要调用腾讯的地址
        String url = "https://apis.map.qq.com/ws/direction/v1/driving/?from={from}&to={to}&key={key}";
        //封装要传递的参数
        Map<String, String> map = new HashMap<>();
        BigDecimal startPointLongitude = form.getStartPointLongitude();
        BigDecimal startPointLatitude = form.getStartPointLatitude();
        String start = startPointLatitude.toString() + "," + startPointLongitude.toString();
        map.put("from", start);
        BigDecimal endPointLongitude = form.getEndPointLongitude();
        BigDecimal endPointLatitude = form.getEndPointLatitude();
        String end = endPointLatitude.toString() + "," + endPointLongitude.toString();
        map.put("to", end);
        map.put("key", key);
        System.out.println("map是"+map);
        //类型是返回的类型
        //getIntValue()方法会:
        //根据key找到对应的value
        //尝试将value转成int类型
        //如果转换失败或key不存在则返回0
        //FastJSON!!!
        JSONObject result = restTemplate.getForObject(url, JSONObject.class, map);
        System.out.println("返回的result是"+result);
        //状态码，0为正常，其它为异常，详细请参阅状态码说明
        if (result.getIntValue("status") != 0) {
            throw new GuiguException(ResultCodeEnum.MAP_FAIL);
        }
        //result(object)搜索结果
        //      routes(array)路线方案（设置get_mp=1时可返回最多3条）
        //选择默认得到的第一条
        //取JSONObject结果，叫result的 再取result下面的Array类型的叫routes的，再取routes里下标为0 也就是第一个 取的返回的全是JSON
        //getJSONObject("key")
        //用于获取一个嵌套的JSON对象
        //如果key对应的值不是对象类型，会报错
        //如果key不存在，返回null
        //getJSONArray("key")
        //用于获取一个JSON数组
        //返回的JSONArray可以用下标访问元素
        //如果key对应的值不是数组类型，会报错
        //如果key不存在，返回null
        JSONObject route =
                result.getJSONObject("result").getJSONArray("routes").getJSONObject(0);
        System.out.println("取到的route"+route);
        DrivingLineVo drivingLineVo = new DrivingLineVo();
        //取JSON里叫distance的value封装为BigDecimal类型赋予drivingLineVo.setDistance 一般都BigDecimal
/*        drivingLineVo.setDistance(route.getBigDecimal("distance")
                        .divideToIntegralValue(new BigDecimal(1000))
                        .setScale(2, RoundingMode.HALF_DOWN));*/
        drivingLineVo.setDistance(route.getBigDecimal("distance")
                .divide(new BigDecimal(1000), 2, RoundingMode.HALF_DOWN));  // 保留两位小数
        drivingLineVo.setDuration(route.getBigDecimal("duration"));
        drivingLineVo.setPolyline(route.getJSONArray("polyline"));
        System.out.println("封装的drivingLineVo"+drivingLineVo);
        return drivingLineVo;
    }
}
