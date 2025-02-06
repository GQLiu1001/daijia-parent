package com.atguigu.daijia.map.repository;

import com.atguigu.daijia.model.entity.map.OrderServiceLocation;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderServiceLocationRepository extends MongoRepository<OrderServiceLocation, String> {

    List<OrderServiceLocation> findByOrderIdOrderByCreateTimeAsc(Long orderId);
}
