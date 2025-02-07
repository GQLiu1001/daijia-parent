package com.atguigu.daijia.driver.service.impl;

import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.driver.client.CosFeignClient;
import com.atguigu.daijia.driver.config.MinioProperties;
import com.atguigu.daijia.driver.service.FileService;
import com.atguigu.daijia.model.vo.driver.CosUploadVo;
import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class FileServiceImpl implements FileService {
    @Resource
    private MinioProperties minioProperties;

    @Override
    public String upload(MultipartFile file) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        // 创建Minio客户端
        MinioClient minioClient = MinioClient.builder()
                .endpoint(minioProperties.getEndpointUrl())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .build();

            // 检查桶是否存在
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(minioProperties.getBucketName()).build());
            if (!found) {
                // 如果桶不存在，创建桶
                log.info("Bucket does not exist. Creating new bucket.");
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(minioProperties.getBucketName()).build());
            } else {
                log.info("Bucket '{}' already exists.", minioProperties.getBucketName());
            }
            InputStream inputStream = file.getInputStream();
            // 获取文件扩展名并生成唯一文件名
            String extFileName = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
            String fileName = UUID.randomUUID().toString().replace("-", "") + extFileName;
            minioClient.putObject(
                    PutObjectArgs.builder().bucket(minioProperties.getBucketName())
                            .object(fileName)
                            .stream(inputStream, file.getSize(), -1)
                            .build());
            inputStream.close();


            String url =
                    minioClient.getPresignedObjectUrl(
                            GetPresignedObjectUrlArgs.builder()
                                    .method(Method.GET)
                                    .bucket(minioProperties.getBucketName())
                                    .object(fileName)
                                    .expiry(2, TimeUnit.HOURS)
                                    .build());
            System.out.println(url);

            return url;

    }
}

