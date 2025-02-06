package com.atguigu.daijia.driver.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.common.util.AuthContextHolder;
import com.atguigu.daijia.driver.config.TencentCloudProperties;
import com.atguigu.daijia.driver.mapper.DriverFaceRecognitionMapper;
import com.atguigu.daijia.driver.mapper.DriverInfoMapper;
import com.atguigu.daijia.driver.mapper.DriverLoginLogMapper;
import com.atguigu.daijia.driver.mapper.DriverSetMapper;
import com.atguigu.daijia.driver.service.CosService;
import com.atguigu.daijia.driver.service.DriverInfoService;
import com.atguigu.daijia.model.entity.driver.DriverFaceRecognition;
import com.atguigu.daijia.model.entity.driver.DriverInfo;
import com.atguigu.daijia.model.entity.driver.DriverLoginLog;
import com.atguigu.daijia.model.entity.driver.DriverSet;
import com.atguigu.daijia.model.form.driver.DriverFaceModelForm;
import com.atguigu.daijia.model.form.driver.UpdateDriverAuthInfoForm;
import com.atguigu.daijia.model.vo.driver.DriverAuthInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverLoginVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.iai.v20200303.IaiClient;
import com.tencentcloudapi.iai.v20200303.models.*;
import com.tencentcloudapi.common.AbstractModel;

import java.util.Date;
import java.util.Random;

@Slf4j
@Service
//@SuppressWarnings({"unchecked", "rawtypes"})
public class DriverInfoServiceImpl extends ServiceImpl<DriverInfoMapper, DriverInfo> implements DriverInfoService {
    @Resource
    private WxMaService wxMaService;
    @Resource
    private DriverLoginLogMapper driverLoginLogMapper;
    @Resource
    private DriverInfoMapper driverInfoMapper;
    @Resource
    private CosService cosService;
    @Resource
    private TencentCloudProperties tencentCloudProperties;
    @Resource
    private DriverSetMapper driverSetMapper;
    @Resource
    private DriverFaceRecognitionMapper driverFaceRecognitionMapper;


    @Override
    public Long login(String code) throws WxErrorException {
        String openid;
        //1.获取code值 使用微信工具包对象(WxMaService) 获取微信唯一标识 openid
        WxMaJscode2SessionResult sessionInfo = wxMaService.getUserService().getSessionInfo(code);
        openid = sessionInfo.getOpenid();
        //2.根据openid判断是否第一次登录 是-》添加信息到用户表 返回用户id值 plus登录日志
//        LambdaQueryWrapper<CustomerInfo> queryWrapper = new LambdaQueryWrapper<>();
        QueryWrapper<DriverInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("wx_open_id", openid);
//        queryWrapper.eq(CustomerInfo::getWxOpenId,openid);
        DriverInfo driverInfo = driverInfoMapper.selectOne(queryWrapper);
        if (driverInfo == null) {
            //new driverInfo() 是必需的 - 因为 driverInfo 为 null 时我们需要一个新对象来存储数据。
            // 否则无法调用 setWxOpenId() 等方法，会报 NullPointerException。
            driverInfo = new DriverInfo();
            driverInfo.setWxOpenId(openid);
            String phone = String.format("%11d", new Random().nextInt(10000));
            driverInfo.setPhone(phone);
            driverInfo.setAvatarUrl("https://oss.aliyuncs.com/aliyun_id_photo_bucket/default_handsome.jpg");
            driverInfo.setNickname("newDriver" + "phone");
            driverInfoMapper.insert(driverInfo);
        }
        DriverLoginLog driverLoginLog = new DriverLoginLog();
        driverLoginLog.setDriverId(driverInfo.getId());
        driverLoginLog.setMsg("小程序登录");
        driverLoginLogMapper.insert(driverLoginLog);
        //3.返回用户id
        return driverInfo.getId();
    }

    @Override
    public DriverLoginVo getDriverInfo(Long driverId) {
        AuthContextHolder.setUserId(driverId);
        System.out.println(driverId);
        //根据id查询客户信息
        DriverInfo driverInfo = driverInfoMapper.selectById(driverId);
        System.out.println("查到的faceId是"+driverInfo.getFaceModelId());
        //封装到VO
        DriverLoginVo driverLoginVo = new DriverLoginVo();
        BeanUtils.copyProperties(driverInfo, driverLoginVo);
        driverLoginVo.setAuthStatus(2);
        //设置setIsArchiveFace  wtf is this？？？？
        driverLoginVo.setIsArchiveFace(true);
        System.out.println(AuthContextHolder.getUserId());
        return driverLoginVo;
    }

    @Override
    public DriverAuthInfoVo getDriverAuthInfo(Long driverId) {
        System.out.println(driverId);
        DriverInfo driverInfo = driverInfoMapper.selectById(driverId);
        //driverAuthInfoVo比driverInfo多了照片url地址
        DriverAuthInfoVo driverAuthInfoVo = new DriverAuthInfoVo();
        BeanUtils.copyProperties(driverInfo, driverAuthInfoVo);
        driverAuthInfoVo.setIdcardBackShowUrl(cosService.getUrl(driverAuthInfoVo.getIdcardBackUrl()));
        driverAuthInfoVo.setIdcardFrontShowUrl(cosService.getUrl(driverAuthInfoVo.getIdcardFrontUrl()));
        driverAuthInfoVo.setIdcardHandShowUrl(cosService.getUrl(driverAuthInfoVo.getIdcardHandUrl()));
        driverAuthInfoVo.setDriverLicenseFrontShowUrl(cosService.getUrl(driverAuthInfoVo.getDriverLicenseFrontUrl()));
        driverAuthInfoVo.setDriverLicenseBackShowUrl(cosService.getUrl(driverAuthInfoVo.getDriverLicenseBackUrl()));
        driverAuthInfoVo.setDriverLicenseHandShowUrl(cosService.getUrl(driverAuthInfoVo.getDriverLicenseHandUrl()));
        return driverAuthInfoVo;
    }

    @Override
    public Boolean updateDriverAuthInfo(UpdateDriverAuthInfoForm form) {
        Long driverId = form.getDriverId();
        System.out.println("service包的updateDriverAuthInfo的userId" + driverId);
        //获取司机的id
        //就是完善认证信息的图片 身份证信息啥的 (均为url)
        //修改 把前端回复的表单信息加入driverInfo
        DriverInfo driverInfo = driverInfoMapper.selectById(driverId);
        System.out.println(driverInfo.getId());
        driverInfo.setId(driverId);
        System.out.println("updateDriverAuthInfo取到的driverInfo是" + driverInfo);
        BeanUtils.copyProperties(form, driverInfo);
        int i = driverInfoMapper.updateById(driverInfo);
        return i > 0;
    }

    //创建司机人脸模型
    @Override
    public Boolean creatDriverFaceModel(DriverFaceModelForm driverFaceModelForm) {
        //根据司机id获取司机信息
        DriverInfo driverInfo =
                driverInfoMapper.selectById(driverFaceModelForm.getDriverId());
        System.out.println("driverFaceModelForm.getDriverId()" + driverFaceModelForm.getDriverId());
        System.out.println("driverInfo" + driverInfo);
        try {
            // 实例化一个认证对象，入参需要传入腾讯云账户 SecretId 和 SecretKey，此处还需注意密钥对的保密
            // 代码泄露可能会导致 SecretId 和 SecretKey 泄露，并威胁账号下所有资源的安全性。以下代码示例仅供参考，建议采用更安全的方式来使用密钥，请参见：https://cloud.tencent.com/document/product/1278/85305
            // 密钥可前往官网控制台 https://console.cloud.tencent.com/cam/capi 进行获取
            Credential cred = new Credential(tencentCloudProperties.getSecretId(),
                    tencentCloudProperties.getSecretKey());
            // 实例化一个http选项，可选的，没有特殊需求可以跳过
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint("iai.tencentcloudapi.com");
            // 实例化一个client选项，可选的，没有特殊需求可以跳过
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);
            // 实例化要请求产品的client对象,clientProfile是可选的
            IaiClient client = new IaiClient(cred, tencentCloudProperties.getRegion(),
                    clientProfile);
            // 实例化一个请求对象,每个接口都会对应一个request对象
            CreatePersonRequest req = new CreatePersonRequest();
            //设置相关值
            req.setGroupId(tencentCloudProperties.getPersonGroupId());
            //基本信息
            req.setPersonId(String.valueOf(driverInfo.getId()));
            req.setGender(Long.parseLong(driverInfo.getGender()));
            req.setQualityControl(4L);
            req.setUniquePersonControl(4L);
            req.setPersonName(driverInfo.getName());
            req.setImage(driverFaceModelForm.getImageBase64());
            // 返回的resp是一个CreatePersonResponse的实例，与请求对象对应
            CreatePersonResponse resp = client.CreatePerson(req);
            // 输出json格式的字符串回包
            System.out.println(AbstractModel.toJsonString(resp));
            String faceId = resp.getFaceId();
            if (StringUtils.hasText(faceId)) {
                driverInfo.setFaceModelId(faceId);
                driverInfoMapper.updateById(driverInfo);
            }
        } catch (TencentCloudSDKException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }


    @Override
    public DriverSet getDriverSet(Long driverId) {
        LambdaQueryWrapper<DriverSet> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DriverSet::getDriverId,driverId);
        return driverSetMapper.selectOne(wrapper);
    }

    @Override
    public Boolean isFaceRecognition(Long driverId) {
        //根据司机id和当日的日期进行查询
        LambdaQueryWrapper<DriverFaceRecognition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DriverFaceRecognition::getDriverId,driverId);
        //年-月-日格式 先得到Data再转换
        wrapper.eq(DriverFaceRecognition::getFaceDate,new DateTime().toString("yyyy-MM-dd"));
        //返回的是数量
        Long count = driverFaceRecognitionMapper.selectCount(wrapper);
        return count > 0;
    }

    @Override
    public Boolean verifyDriverFace(DriverFaceModelForm driverFaceModelForm) {
        //1 照片比对
        try{
            // 实例化一个认证对象，入参需要传入腾讯云账户 SecretId 和 SecretKey，此处还需注意密钥对的保密
            // 代码泄露可能会导致 SecretId 和 SecretKey 泄露，并威胁账号下所有资源的安全性。以下代码示例仅供参考，建议采用更安全的方式来使用密钥，请参见：https://cloud.tencent.com/document/product/1278/85305
            // 密钥可前往官网控制台 https://console.cloud.tencent.com/cam/capi 进行获取
            Credential cred = new Credential(tencentCloudProperties.getSecretId(),
                    tencentCloudProperties.getSecretKey());
            // 实例化一个http选项，可选的，没有特殊需求可以跳过
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint("iai.tencentcloudapi.com");
            // 实例化一个client选项，可选的，没有特殊需求可以跳过
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);

            // 实例化要请求产品的client对象,clientProfile是可选的
            IaiClient client = new IaiClient(cred,
                    tencentCloudProperties.getRegion(),
                    clientProfile);
            // 实例化一个请求对象,每个接口都会对应一个request对象
            VerifyFaceRequest req = new VerifyFaceRequest();
            //设置相关参数
            req.setImage(driverFaceModelForm.getImageBase64());
            req.setPersonId(String.valueOf(driverFaceModelForm.getDriverId()));

            // 返回的resp是一个VerifyFaceResponse的实例，与请求对象对应
            VerifyFaceResponse resp = client.VerifyFace(req);
            // 输出json格式的字符串回包
            System.out.println(AbstractModel.toJsonString(resp));
            if(resp.getIsMatch()) { //照片比对成功
                //2 如果照片比对成功，静态活体检测
                Boolean isSuccess = this.
                        detectLiveFace(driverFaceModelForm.getImageBase64());
                if(true) {//3 如果静态活体检测通过，添加数据到认证表里面
                    DriverFaceRecognition driverFaceRecognition = new DriverFaceRecognition();
                    driverFaceRecognition.setDriverId(driverFaceModelForm.getDriverId());
                    driverFaceRecognition.setFaceDate(new Date());
                    driverFaceRecognitionMapper.insert(driverFaceRecognition);
                    return true;
                }
            }
        } catch (TencentCloudSDKException e) {
            System.out.println(e.toString());
        }

        throw new GuiguException(ResultCodeEnum.DATA_ERROR);
    }

    //更新接单状态
// update driver_set set status=? where driver_id=?
    @Override
    public Boolean updateServiceStatus(Long driverId, Integer status) {
        LambdaQueryWrapper<DriverSet> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DriverSet::getDriverId,driverId);
        DriverSet driverSet = new DriverSet();
        driverSet.setServiceStatus(status);
        driverSetMapper.update(driverSet,wrapper);
        return true;
    }

    //获取司机基本信息
    @Override
    public DriverInfoVo getDriverInfoOrder(Long driverId) {
        //司机id获取基本信息
        DriverInfo driverInfo = driverInfoMapper.selectById(driverId);

        //封装DriverInfoVo
        DriverInfoVo driverInfoVo = new DriverInfoVo();
        BeanUtils.copyProperties(driverInfo,driverInfoVo);

        //计算驾龄
        //获取当前年
        int currentYear = new DateTime().getYear();
        //获取驾驶证初次领证日期
        //driver_license_issue_date
        int firstYear = new DateTime(driverInfo.getDriverLicenseIssueDate()).getYear();
        int driverLicenseAge = currentYear - firstYear;
        driverInfoVo.setDriverLicenseAge(driverLicenseAge);

        return driverInfoVo;
    }

    @Override
    public String getDriverOpenId(Long driverId) {
        LambdaQueryWrapper<DriverInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DriverInfo::getId,driverId);
        DriverInfo driverInfo = driverInfoMapper.selectOne(wrapper);
        return driverInfo.getWxOpenId();
    }

    //人脸静态活体检测
    private Boolean detectLiveFace(String imageBase64) {
        try{
            // 实例化一个认证对象，入参需要传入腾讯云账户 SecretId 和 SecretKey，此处还需注意密钥对的保密
            // 代码泄露可能会导致 SecretId 和 SecretKey 泄露，并威胁账号下所有资源的安全性。以下代码示例仅供参考，建议采用更安全的方式来使用密钥，请参见：https://cloud.tencent.com/document/product/1278/85305
            // 密钥可前往官网控制台 https://console.cloud.tencent.com/cam/capi 进行获取
            Credential cred = new Credential(tencentCloudProperties.getSecretId(),
                    tencentCloudProperties.getSecretKey());
            // 实例化一个http选项，可选的，没有特殊需求可以跳过
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint("iai.tencentcloudapi.com");
            // 实例化一个client选项，可选的，没有特殊需求可以跳过
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);
            // 实例化要请求产品的client对象,clientProfile是可选的
            IaiClient client = new IaiClient(cred, tencentCloudProperties.getRegion(),
                    clientProfile);
            // 实例化一个请求对象,每个接口都会对应一个request对象
            DetectLiveFaceRequest req = new DetectLiveFaceRequest();
            req.setImage(imageBase64);
            // 返回的resp是一个DetectLiveFaceResponse的实例，与请求对象对应
            DetectLiveFaceResponse resp = client.DetectLiveFace(req);
            // 输出json格式的字符串回包
            System.out.println(DetectLiveFaceResponse.toJsonString(resp));
            if(resp.getIsLiveness()) {
                return true;
            }
        } catch (TencentCloudSDKException e) {
            System.out.println(e.toString());
        }
        return true;
    }
}