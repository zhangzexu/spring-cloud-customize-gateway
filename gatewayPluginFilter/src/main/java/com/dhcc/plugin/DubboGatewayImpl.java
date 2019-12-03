package com.dhcc.plugin;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ReferenceConfig;
import com.alibaba.fastjson.JSONObject;
import org.apache.dubbo.demo.DemoService;

/**
 * @ClassName DubboGatewayImpl
 * @Desription
 * @Author 张泽旭
 * @Date 2019/11/29 15:10
 * @Version V1.0
 */
public class DubboGatewayImpl implements GatewayPlugin{

    @Override
    public JSONObject send(String url, byte[] Bytes) {
        ApplicationConfig application = new ApplicationConfig();
        application.setName("demo-consumer");
        ReferenceConfig<DemoService> reference = new ReferenceConfig<DemoService>();
        reference.setApplication(application);
        reference.setUrl(url);
        reference.setTimeout(5000);
        reference.setInterface(DemoService.class);
        DemoService demoService = reference.get();
        String hello = demoService.sayHello("world");
        JSONObject data = new JSONObject();
        data.put("code",200);
        data.put("result",hello);
        return data;
    }
}
