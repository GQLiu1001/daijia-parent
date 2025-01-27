package com.atguigu.daijia.driver.controller;

import com.atguigu.daijia.common.login.GuiguLogin;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.driver.service.FileService;
import com.atguigu.daijia.model.vo.driver.CosUploadVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Reference;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "上传管理接口")
@RestController
@RequestMapping("file")
public class FileController {
    @Resource
    private FileService fileService;
    //文件上传接口
//    @GuiguLogin
    @Operation(summary = "上传文件")
    @PostMapping("/upload") //url 是 上传地址
    public Result<CosUploadVo> upload(@RequestPart("file") MultipartFile file,
                            @RequestParam(name = "path",defaultValue = "auth") String url) {
        System.out.println(url);
        CosUploadVo vo = fileService.upload(file,url);
        return Result.ok(vo);
    }

}
