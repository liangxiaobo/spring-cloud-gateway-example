
Spring Cloud Gateway是Spring Cloud官方推出的第二代网关框架，取代Zuul网关。网关作为流量的，在微服务系统中有着非常作用，网关常见的功能有路由转发、权限校验、限流控制等作用。

## 项目结构

| 项目 | 端口 |描述 |
| ------ | ------ | ------ |
| eureka-server | 8761 | 服务的注册与发现 |
| service-one  | 8081 | 服务  |
| gateway-client  | 8080 | 网关 gateway  |


## eureka-server
eureka-server项目非常简单 引入
```xml
<dependency>
         <groupId>org.springframework.cloud</groupId>
         <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
</dependency>
```
启动类里面
```java
@EnableEurekaServer
@SpringBootApplication
public class EurekaServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
```
配置文件
```xml
spring:
  application:
    name: eureka-server

server:
  port: 8761
eureka:
  instance:
    hostname: localhostname
  client:
    fetch-registry: false
    register-with-eureka: false
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

## service-one 项目
搭建非常简单，添加依赖
```xml
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
```
启动类里面
```java
@EnableEurekaClient
@SpringBootApplication
public class ServiceOneApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceOneApplication.class, args);
    }
}
```
配置文件
```xml
spring:
  application:
    name: service-one
server:
  port: 8081

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```
创建类控制器 UserController，http://localhost:8081/user/who
```java
@RequestMapping("/user")
@RestController
public class UserController {

    @RequestMapping("who")
    public String who() {
        return "my name is liangwang";
    }
}
```
创建类控制器OrderController，http://localhost:8081/order/info
```java
@RequestMapping("/order")
@RestController
public class OrderController {

    @RequestMapping("/info")
    public String orderInfo() {
        return "order info date : " + new Date().toString();
    }
}
```
## gateway-client项目
使用的是Finchley.SR2版本的，其中已经包含了 ```spring-boot-starter-webflux```
添加依赖
```xml
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-gateway</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
```
使用RouteLocator的Bean进行路由转发，将请求进行处理，最后转发到目标的下游服务
```java
@SpringBootApplication
public class GatewayClientApplication {

    @Value("${test.uri}")
    private String uri;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                //basic proxy
                .route(r -> r.path("/order/**")
                        .uri(uri)
                ).build();
    }

    public static void main(String[] args) {
        SpringApplication.run(GatewayClientApplication.class, args);
    }
}
```
> 上面的代码里是在访问http://localhost:8080/order/**的时候，网关转发到http://service-one:8081/order/**，service-one服务在eureka中有注册，最终是对应服务的ip:port

使用配置文件
application.yml
```xml
test:
  uri: lb://service-one
spring:
  application:
    name: gateway-client
  cloud:
    gateway:
      routes:
      - id: route_service_one
        uri: ${test.uri} # uri以lb://开头（lb代表从注册中心获取服务），后面接的就是你需要转发到的服务名称
        predicates:
        - Path=/user/**

server:
  port: 8080

logging:
  level:
    org.springframework.cloud.gateway: TRACE
    org.springframework.http.server.reactive: DEBUG
    org.springframework.web.reactive: DEBUG
    reactor.ipc.netty: DEBUG
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
  instance:
    prefer-ip-address: true
```
其中test.uri是我自定义的属性，uri以lb://开头（lb代表从注册中心获取服务），后面接的就是你需要转发到的服务名称，按照上面的配置是http://localhost:8080/usr/** => http://service-one:8081/user/** 到此项目搭建完成，接下来测试了，依次启动eureka-server、service-one、gateway-client
访问 
* http://localhost:8080/user/who
* http://localhost:8080/order/info

![WX20181113-180248@2x.png](https://upload-images.jianshu.io/upload_images/2151905-959e55f6a9b7a3c1.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/760)

![WX20181113-180339@2x.png](https://upload-images.jianshu.io/upload_images/2151905-677ac37e6aedef6c.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/760)

### 不启用注册中心
即使集成了eureka-client也不想使用注册中心服务，可以关闭
```xml
eureka.client.enabled=false
```
### StripPrefix属性的使用

按照上面的配置，每一个路由只能对应一个控制器的转发，不够灵活，假如我想让userapi的请求都转到service-one服务，比如：
* http://localhost:8080/userapi/user/who => http://localhost:8081/user/who
* http://localhost:8080/userapi/order/info => http://localhost:8081/order/info
在路由配置上增加 ```StripPrefix=1```
```xml
spring:
  application:
    name: gateway-client
  cloud:
    gateway:
      routes:
      - id: route_service_one
        uri: ${test.uri} # uri以lb://开头（lb代表从注册中心获取服务），后面接的就是你需要转发到的服务名称
        predicates:
        - Path=/userapi/**
        filters:
        - StripPrefix=1 # 表示在转发时去掉userapi
```
修改完配置，重启gateway-client项目：
![](https://upload-images.jianshu.io/upload_images/2151905-7f47d665098e8920.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/760)

![](https://upload-images.jianshu.io/upload_images/2151905-26d08c50e6d1e4c4.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/760)

## 使用Hystrix
在gateway-client项目中引入依赖
```xml
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-hystrix</artifactId>
        </dependency>
```
在spring cloud gateway中可以使用Hystrix。Hystrix是 spring cloud中一个服务熔断降级的组件，在微服务系统有着十分重要的作用。
Hystrix是 spring cloud gateway中是以filter的形式使用的，代码如下：
```java
@SpringBootApplication
public class GatewayClientApplication {

    @Value("${test.uri}")
    private String uri;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                //basic proxy
                .route(r -> r.path("/order/**")
                        .uri(uri)
                )
                .route(r -> r.path("/user/**")
                        .filters(f -> f
                                .hystrix(config -> config
                                        .setName("myserviceOne")
                                        .setFallbackUri("forward:/user/fallback")))
                        .uri(uri)).build();
    }

    public static void main(String[] args) {
        SpringApplication.run(GatewayClientApplication.class, args);
    }
}
```
上面代码中添加了一个路由并配置了hystrix的fallbackUri，新添加一个FallBackController控制器
```java
@RestController
public class FallBackController {
    @RequestMapping("/user/fallback")
    public Mono<String> fallback() {
        return Mono.just("service error, jump fallback");
    }
}
```
重启gateway-client项目，并关闭service-one服务，在浏览器访问 http://localhost:8080/user/who

![](https://upload-images.jianshu.io/upload_images/2151905-e3f1ed511625e68b.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/760)

**使用yml配置Hystrix**
```xml
spring:
  application:
    name: gateway-client
  cloud:
    gateway:
      routes:
      - id: route_service_one
        uri: ${test.uri} # uri以lb://开头（lb代表从注册中心获取服务），后面接的就是你需要转发到的服务名称
        predicates:
        - Path=/userapi/**
        filters:
        - StripPrefix=1 # 表示在转发时去掉userapi

      - id: userapi2_route
        uri: ${test.uri}
        predicates:
        - Path=/userapi2/**
        filters:
        - StripPrefix=1
        - name: Hystrix
          args:
            name: myfallbackcmd
            fallbackUri: forward:/user/fallback
```
在配置中增加了一个新的路由userapi2_route，还配置了Hystrix，当发生错误时会转发到fallbackUri，测试访问 http://localhost:8080/userapi2/order/info 

![](https://upload-images.jianshu.io/upload_images/2151905-e6e982fc19c85124.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/760)

> reference
[Hystrix wiki](https://github.com/Netflix/Hystrix/wiki/Configuration)<br/>
[Spring Cloud Gateway](https://spring.io/projects/spring-cloud-gateway#overview)<br/>
[Redis RateLimiter](https://cloud.spring.io/spring-cloud-static/spring-cloud-gateway/2.1.0.M1/single/spring-cloud-gateway.html#_redis_ratelimiter)<br/>
[转载他人的写好的Redis RateLimiter实现](https://segmentfault.com/a/1190000015442572)


