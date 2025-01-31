package com.atguigu.daijia.driver.mapper;

import com.atguigu.daijia.model.entity.driver.DriverSet;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DriverSetMapper extends BaseMapper<DriverSet> {

    @Select("SELECT * FROM driver_set WHERE driver_id = #{driverId}")
    DriverSet selectSetById(Long driverId);
}
