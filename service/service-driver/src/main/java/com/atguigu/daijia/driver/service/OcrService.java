package com.atguigu.daijia.driver.service;

import com.atguigu.daijia.model.vo.driver.DriverLicenseOcrVo;
import com.atguigu.daijia.model.vo.driver.IdCardOcrVo;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface OcrService {


    IdCardOcrVo idCardOcr(MultipartFile file) throws IOException, TencentCloudSDKException;

    DriverLicenseOcrVo driverLicenseOcr(MultipartFile file) throws TencentCloudSDKException, IOException;
}
