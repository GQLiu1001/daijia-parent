package com.atguigu.daijia.driver.service;

import com.atguigu.daijia.model.form.order.OrderMonitorForm;
import io.minio.errors.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public interface MonitorService {

    Boolean upload(MultipartFile file, OrderMonitorForm orderMonitorForm) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException;
}
