package com.atguigu.daijia.driver.service;

import com.atguigu.daijia.model.form.map.UpdateDriverLocationForm;
import com.atguigu.daijia.model.form.map.UpdateOrderLocationForm;
import com.atguigu.daijia.model.vo.map.OrderLocationVo;

public interface LocationService {


    Boolean updateDriverLocation(UpdateDriverLocationForm updateDriverLocationForm);

    Boolean updateOrderLocationToCache(UpdateOrderLocationForm updateOrderLocationForm);

    OrderLocationVo getCacheOrderLocation(Long orderId);
}
