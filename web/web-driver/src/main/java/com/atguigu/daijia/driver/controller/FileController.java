package com.atguigu.daijia.driver.controller;

import com.atguigu.daijia.common.login.GuiguLogin;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.driver.service.CosService;
import com.atguigu.daijia.driver.service.FileService;
import com.atguigu.daijia.model.vo.driver.CosUploadVo;
import io.minio.errors.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Reference;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Tag(name = "上传管理接口")
@RestController
@RequestMapping("file")
public class FileController {
    @Resource
    private FileService fileService;

    @Autowired
    private CosService cosService;

    //文件上传接口
//    @Operation(summary = "上传")
//    //@GuiguLogin
//    @PostMapping("/upload")
//    public Result<String> upload(@RequestPart("file") MultipartFile file,
//                                      @RequestParam(name = "path",defaultValue = "auth") String path) {
//        CosUploadVo cosUploadVo = cosService.upload(file,path);
//        return Result.ok(cosUploadVo.getShowUrl());
//    }

    @Operation(summary = "上传")
    @PostMapping("/upload")
    public Result<String> upload(@RequestPart("file") MultipartFile file) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        String url = fileService.upload(file);
        return Result.ok(url);
    }

}
