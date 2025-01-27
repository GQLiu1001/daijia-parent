package com.atguigu.daijia.driver.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.daijia.driver.config.TencentCloudProperties;
import com.atguigu.daijia.driver.service.CosService;
import com.atguigu.daijia.model.vo.driver.CosUploadVo;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.model.*;
import com.qcloud.cos.region.Region;
import com.tencentcloudapi.common.Credential;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class CosServiceImpl implements CosService {

    @Resource
    private TencentCloudProperties properties;
    @Autowired
    private TencentCloudProperties tencentCloudProperties;
    public COSClient getCosClient(){
        // 1 初始化用户身份信息（secretId, secretKey）。
        // SECRETID 和 SECRETKEY 请登录访问管理控制台 https://console.cloud.tencent.com/cam/capi 进行查看和管理
        String secretId = properties.getSecretId();
        String secretKey = properties.getSecretKey();
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        // 2 设置 bucket 的地域, COS 地域的简称请参见 https://cloud.tencent.com/document/product/436/6224
        // clientConfig 中包含了设置 region, https(默认 http), 超时, 代理等 set 方法, 使用可参见源码或者常见问题 Java SDK 部分。
        Region region = new Region(properties.getRegion());
        ClientConfig config = new ClientConfig(region);
        // 这里建议设置使用 https 协议
        // 从 5.6.54 版本开始，默认使用了 https
        config.setHttpProtocol(HttpProtocol.https);
        // 3 生成 cos 客户端。
        COSClient cosClient = new COSClient(cred, config);
        return cosClient;
    }
    @Override
    public CosUploadVo upload(MultipartFile file ,String url) {

        //元数据信息
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentType(file.getContentType());
        objectMetadata.setContentLength(file.getSize());
        objectMetadata.setContentEncoding("utf-8");
        // 01.jpg 假如传入的图片
        // /driver/auth/O09173.jpg  uploadPath在腾讯云的路径
        String fileType = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
        String uploadPath ="/driver/"+url+"/"+ UUID.randomUUID().toString().replaceAll("-","")+fileType;
        PutObjectRequest putObjectRequest = null;
        try{
            //获取桶名
            putObjectRequest = new PutObjectRequest(tencentCloudProperties.getBucketPrivate(),
                    uploadPath,
                    file.getInputStream(),
                    objectMetadata);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        putObjectRequest.setStorageClass(StorageClass.Standard);
        COSClient cosClient = getCosClient();
        PutObjectResult putObjectResult = cosClient.putObject(putObjectRequest);//上传文件
        log.info(JSON.toJSONString(putObjectResult));
        cosClient.shutdown();
        //上传完返回一个VO对象 一个路径 一个回显地址
        CosUploadVo cosUploadVo = new CosUploadVo();
        cosUploadVo.setUrl(uploadPath);//上传路径 （腾讯云里的）
        //回显地址
        String url1 = this.getUrl(uploadPath);
        cosUploadVo.setShowUrl(url1);
        return cosUploadVo;
    }

    @Override
    public String getUrl(String path) {
        COSClient cosClient =this.getCosClient();
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                tencentCloudProperties.getBucketPrivate(), path , HttpMethodName.GET
        );
        Date expirationDate = new Date(System.currentTimeMillis() + 30 * 60 * 1000);
        request.setExpiration(expirationDate);
        URL url = cosClient.generatePresignedUrl(request);
        cosClient.shutdown();
        return url.toString();
    }
}
