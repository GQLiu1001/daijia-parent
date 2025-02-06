package com.atguigu.daijia.driver.service;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.model.vo.driver.CosUploadVo;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {


    String upload(MultipartFile file);
}
