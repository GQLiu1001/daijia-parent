package com.atguigu.daijia.map.controller;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.map.service.MapService;
import com.atguigu.daijia.model.form.map.CalculateDrivingLineForm;
import com.atguigu.daijia.model.vo.map.DrivingLineVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "地图API接口管理")
@RestController
@RequestMapping("/map")
@SuppressWarnings({"unchecked", "rawtypes"})
public class MapController {
    @Resource
    private MapService mapService;

    @Operation(summary = "计算驾驶路线")
    @PostMapping("/calculateDrivingLine/{userId}")
    public Result<DrivingLineVo> calculateDrivingLine(@RequestBody CalculateDrivingLineForm form ,@PathVariable("userId") Long userId) {
        DrivingLineVo vo = mapService.calculateDrivingLine(form,userId);
        return Result.ok(vo);
    }


}

