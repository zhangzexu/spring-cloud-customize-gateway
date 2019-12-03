package com.dhcc.plugin;

import com.alibaba.fastjson.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @Title
 * @ClassName SoapGatewayPluginImpl
 * @Desription
 * @Author zhangzexu
 * @Date 2019/12/2 17:14
 * @Version V1.0
 */
public class SoapGatewayPluginImpl implements GatewayPlugin{

    @Override
    public JSONObject send(String url, byte[] bytes){
        JSONObject data = new JSONObject();
        try {
            URL soapurl = new URL("http://fy.webxml.com.cn/webservices/EnglishChinese.asmx?wsdl");
            //第二步：打开一个通向服务地址的连接
            HttpURLConnection connection = (HttpURLConnection) soapurl.openConnection();
            //第三步：设置参数
            //3.1发送方式设置：POST必须大写
            connection.setRequestMethod("POST");
            //3.2设置数据格式：content-type
            connection.setRequestProperty("content-type", "text/xml;charset=utf-8");
            //3.3设置输入输出，因为默认新创建的connection没有读写权限，
            connection.setDoInput(true);
            connection.setDoOutput(true);

            //第四步：组织SOAP数据，发送请求
            //将信息以流的方式发送出去

            OutputStream os = connection.getOutputStream();
            os.write(bytes);
            //第五步：接收服务端响应，打印
            int responseCode = connection.getResponseCode();

            /**
             * 获取服务响应结果
             */
            InputStream is = connection.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String temp = null;
            while (null != (temp = br.readLine())) {
                sb.append(temp);
            }
            data.put("code",responseCode);
            data.put("data",sb.toString());
            is.close();
            isr.close();
            br.close();
            os.close();
        }catch (IOException e){
            data.put("code",500);
            data.put("data",e.getLocalizedMessage());
        }
        return data;
    }


}
