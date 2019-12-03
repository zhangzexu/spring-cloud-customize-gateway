package org.apache.dubbo.demo;

import java.util.concurrent.CompletableFuture;

/**
 * @Title
 * @ClassName DemoService
 * @Desription
 * @Author yangxiaoxiao
 * @Date 2019/11/27 16:51
 * @Version V1.0
 */
public interface DemoService {
    String sayHello(String var1);

    default CompletableFuture<String> sayHelloAsync(String name) {
        return CompletableFuture.completedFuture(this.sayHello(name));
    }
}
