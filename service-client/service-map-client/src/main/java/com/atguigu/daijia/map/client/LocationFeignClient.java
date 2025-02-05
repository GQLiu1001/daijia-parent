package com.atguigu.daijia.map.client;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.model.form.map.SearchNearByDriverForm;
import com.atguigu.daijia.model.form.map.UpdateDriverLocationForm;
import com.atguigu.daijia.model.form.map.UpdateOrderLocationForm;
import com.atguigu.daijia.model.vo.map.NearByDriverVo;
import com.atguigu.daijia.model.vo.map.OrderLocationVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(value = "service-map")
public interface LocationFeignClient {

    @PostMapping("/map/location/updateDriverLocation")
    Result<Boolean> updateDriverLocation(@RequestBody UpdateDriverLocationForm updateDriverLocationForm);

    @DeleteMapping("/map/location/removeDriverLocation/{driverId}")
    Result<Boolean> removeDriverLocation(@PathVariable("driverId") Long driverId);

    //搜索附近满足条件的司机
    @PostMapping("/map/location/searchNearByDriver")
    Result<List<NearByDriverVo>> searchNearByDriver(@RequestBody
                                                    SearchNearByDriverForm searchNearByDriverForm);

    @PostMapping("/map/location/updateOrderLocationToCache")
    Result<Boolean> updateOrderLocationToCache(@RequestBody UpdateOrderLocationForm updateOrderLocationForm);

    @GetMapping("/map/location/getCacheOrderLocation/{orderId}")
    Result<OrderLocationVo> getCacheOrderLocation(@PathVariable("orderId") Long orderId);
}