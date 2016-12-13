package com.color.home.utils;

import android.util.Base64;

import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * JAVA版通过appid和key生成的使用SmartWeatherAPI的Url类
 * 调用方法:String apiUrl = SmartWeatherUrlUtil.getInterfaceURL("城市编号","数据类型");
 * 数据类型：实况,预报(3d)，指数
 *
 */
public class SmartWeatherUrlUtil {

    private static final String MAC_NAME = "HmacSHA1";
    private static final String ENCODING = "utf-8";
    private static final String appid = "edb0b7343edefc77";
    private static final String private_key = "8c72cf_SmartWeatherAPI_e87c3f2";
    private static final String url_head = "http://webapi.weather.com.cn/data/?";

    /**
     * 使用  HAC-SHA1 强命方法对 encryptText 进行签名
     * url : 被签名的字符串
     * privatekey : 密钥
     * */
    private static byte[] HmacSHA1Encrypt(String url, String privatekey) throws Exception {
        byte[] data = privatekey.getBytes(ENCODING);
//        根据给定的字节数组构造一个密钥，第二个参数制定一个密钥算法的名称
        SecretKey secretKey = new SecretKeySpec(data,MAC_NAME);
//        生成一个制定 Mac 算法的 Mac 对象
        Mac mac = Mac.getInstance(MAC_NAME);
//        用给定密钥初始化 Mac 对象
        mac.init(secretKey);
        byte[] text = url.getBytes(ENCODING);
//        完成 Mac 操作
        return mac.doFinal(text);
    }

    /**
     * 获取 URL 通过 privatekey 加密后的码
     * */
    private static String getKey(String url , String privatekey) throws Exception {
        byte[] key_bytes = HmacSHA1Encrypt(url,privatekey);
        String base64encodeStr = Base64.encodeToString(key_bytes, Base64.NO_WRAP);
        return URLEncoder.encode(base64encodeStr,ENCODING);
    }


    private static String getInterfaceURL(String areaid, String type, String date)
            throws Exception {
        String keyurl = url_head+"areaid="+areaid+"&type="+type+
                "&date="+date+"&appid=";
        String key = getKey(keyurl+appid,private_key);
        String appid6 = appid.substring(0,6);
        return keyurl+appid6+"&key="+key;
    }
    public static String getInterfaceURL(String areaid, String type){
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmm");
        String date = dateFormat.format(new Date());
        try {
            return getInterfaceURL(areaid,type,date);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }



}
