package com.atguigu.daijia.driver.controller;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.driver.service.OcrService;
import com.atguigu.daijia.model.vo.driver.DriverLicenseOcrVo;
import com.atguigu.daijia.model.vo.driver.IdCardOcrVo;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@Tag(name = "腾讯云识别接口管理")
@RestController
@RequestMapping(value="/ocr")
@SuppressWarnings({"unchecked", "rawtypes"})
public class OcrController {
    @Resource
    private OcrService ocrService;
    @Operation(summary = "身份证识别")
    @PostMapping("/idCardOcr")
    public IdCardOcrVo idCardOcr(@RequestPart MultipartFile file) throws TencentCloudSDKException, IOException {
        return ocrService.idCardOcr(file);
    }
    @Operation(summary = "驾驶证识别")
    @PostMapping("/driverLicenseOcr")
    public DriverLicenseOcrVo driverLicenseOcr(MultipartFile file) throws TencentCloudSDKException, IOException {
        return ocrService.driverLicenseOcr(file);
    }
}

