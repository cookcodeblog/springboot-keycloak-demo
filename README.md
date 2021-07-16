[TOC]

# 在Spring Boot应用中集成Keycloak



## 前言

本文描述了在Spring Boot应用中通过Spring Security集成Keycloak来实现用认证和鉴权。



工具和环境：

- Spring Boot 2.4.0
- Spring Security
- Spring Boot Thymeleaf
- Keycloak 12.0.1



## 引入依赖



### Spring Security依赖

```xml
<dependency>
  <groupId>org.springframework.security</groupId>
  <artifactId>spring-security-test</artifactId>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>org.keycloak</groupId>
  <artifactId>keycloak-spring-boot-starter</artifactId>
</dependency>
```



### Keycloak依赖

```xml
<dependency>
  <groupId>org.keycloak</groupId>
  <artifactId>keycloak-spring-boot-starter</artifactId>
</dependency>

<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.keycloak.bom</groupId>
      <artifactId>keycloak-adapter-bom</artifactId>
      <version>12.0.1</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```



### Thymeleaf依赖

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>
<dependency>
  <groupId>org.thymeleaf.extras</groupId>
  <artifactId>thymeleaf-extras-springsecurity5</artifactId>
</dependency>
```




## 安装Keycloak



以Docker方式安装Keycloak：



```bash
#!/bin/bash

# Create a user defined network
docker network create keycloak-network

# Start a MySQL instance
docker run --name keycloak-mysql \
  -d \
  --net keycloak-network \
  -e MYSQL_DATABASE=keycloak \
  -e MYSQL_USER=keycloak \
  -e MYSQL_PASSWORD=keycloak123 \
  -e MYSQL_ROOT_PASSWORD=keycloak123 \
  mysql:8.0

# Start a Keycloak instance
docker run --name keycloak \
  -d \
  --net keycloak-network \
  -p 8180:8080 \
  -e DB_VENDOR=mysql \
  -e DB_ADDR=keycloak-mysql \
  -e DB_DATABASE=keycloak \
  -e DB_USER=keycloak \
  -e DB_PASSWORD=keycloak123 \
  -e KEYCLOAK_USER=admin \
  -e KEYCLOAK_PASSWORD=admin \
  quay.io/keycloak/keycloak:12.0.1

# check logs
# docker logs -f keycloak
```



说明：

- 采用MySQL来持久化Keycloak配置。
- 设置Keycloak的端口为`8180`。



参见：

- <https://www.keycloak.org/getting-started/getting-started-docker>
- <https://hub.docker.com/r/jboss/keycloak/>



## 在Keycloak上配置

在Keycloak上新建Realm、Client、Role和User：

- 创建一个新Realm - `xdevops`
- 在该Realm下创建一个Client 
  - Client ID - `springboot-keycloak-demo`
  - Root URL - `http://localhost:8080/` （对应的Valid Redirect URL为`http://localhost:8080/*`)
- 在该Realm下创建两个Role
  - `admin` - 管理员
  - `user` - 普通用户
- 在`admin` 角色下创建`william`用户，在`user`角色下创建`john`用户。



参见：

- <https://www.keycloak.org/getting-started/getting-started-docker>



## 构建Spring Boot应用

### 配置Keycloak属性

在`application.yaml`中配置Keycloak属性：

```yaml
keycloak:
  # the name of the realm, required
  realm: xdevops
  # the client-id of the application, required
  resource: springboot-keycloak-demo
  # the base URL of the Keycloak server, required
  auth-server-url: http://localhost:8180/auth
  # establishes if communications with the Keycloak server must happen over HTTPS
  # set to external, meaning that it's only needed for external requests (default value)
  # In production, instead, we should set it to all. Optional
  ssl-required: external
  # prevents the application from sending credentials to the Keycloak server (false is the default value)
  # set it to true whenever we use public clients instead of confidential
  public-client: true
  # the attribute with which to populate the UserPrincipal name
  principal-attribute: preferred_username

```



说明：

- `realm` 为上面创建的Relam。

- `resource `为上面创建的Client ID。

- `auth-server-url` 为Keycloak server的auth url。

- 默认创建的Client的Access Type为`public`，所以这里设置`public-client`为`true`

- `principal-attribute: preferred_username` 表示用Keycloak User的`preferred_username` 属性作为Spring Security Principal的`name`。

  

### Keycloak安全配置



创建一个`SecurityConfig`类：



```java
import org.keycloak.adapters.springboot.KeycloakSpringBootConfigResolver;
import org.keycloak.adapters.springsecurity.KeycloakSecurityComponents;
import org.keycloak.adapters.springsecurity.authentication.KeycloakAuthenticationProvider;
import org.keycloak.adapters.springsecurity.config.KeycloakWebSecurityConfigurerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;

@Configuration
@EnableWebSecurity
@ComponentScan(basePackageClasses = KeycloakSecurityComponents.class)
public class SecurityConfig extends KeycloakWebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        super.configure(http);
        http.authorizeRequests()
                    .antMatchers("/manager").hasRole("admin")
                    .antMatchers("/books").hasAnyRole("user", "admin")
                    .anyRequest().permitAll();
    }

    /**
     * Make sure roles are not prefixed with ROLE_.
     * @param builder
     */
    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder builder) {
        KeycloakAuthenticationProvider provider = keycloakAuthenticationProvider();
        provider.setGrantedAuthoritiesMapper(new SimpleAuthorityMapper());
        builder.authenticationProvider(provider);
    }

    /**
     * Use the Spring Boot application properties file support instead of the default keycloak.json.
     * @return
     */
    @Bean
    public KeycloakSpringBootConfigResolver keycloakConfigResolver() {
        return new KeycloakSpringBootConfigResolver();
    }

    @Override
    protected SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        return new RegisterSessionAuthenticationStrategy(new SessionRegistryImpl());
    }

}
```



说明：

- 访问控制
  - 配置了只有`admin`角色时才能访问`/manager` 端点。
  - 配置了只有`user`或`admin`角色时才能访问`/books`端点。
  - 访问其他端点，不作控制。
- 注入`configureGlobal`，不让Spring Security默认在Role前添加`ROLE_`。
- 注入`keycloakConfigResolver`，让Spring Boot从application properties/yaml 中读取Keycloak配置，而不是从默认的类路径的`key cloak.json`中读取配置。



关于`httpsecurity`的用法参见：

- <https://www.baeldung.com/spring-security-expressions>



### Web层

在`LibraryController`类中定义了两个端点：

- `/books` - 普通用户或管理员都可以浏览图书。
- `/manager` - 管理员才可以管理图书。



```java
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;

@Controller
public class LibraryController {

    private final BookRepository bookRepository;

    public LibraryController(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @GetMapping("/books")
    public String getBooks(Model model, Principal principal) {
        model.addAttribute("books", bookRepository.readAll());
        model.addAttribute("name", principal.getName());
        return "books";
    }

    @GetMapping("/manager")
    public String manageBooks(Model model, HttpServletRequest request) {
        model.addAttribute("books", bookRepository.readAll());
        model.addAttribute("name", SecurityUtils.getIDToken(request).getGivenName());
        return "manager";
    }
}
```



说明：

- `getBooks` 方法演示了直接通过Spring Security Principal获取当前用户的名称，这里是Keycloak User的`preferred_username`。
- `manageBooks` 方法演示了通过一个工具类从request中获取Keycloak User的详细信息。



### Keycloak工具类



```java
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.representations.IDToken;

import javax.servlet.http.HttpServletRequest;

public final class SecurityUtils {

    private SecurityUtils() {

    }

    public static KeycloakSecurityContext getKeycloakSecurityContext(HttpServletRequest request) {
        return (KeycloakSecurityContext) request.getAttribute(KeycloakSecurityContext.class.getName());
    }

    public static IDToken getIDToken(HttpServletRequest request) {
        return SecurityUtils.getKeycloakSecurityContext(request).getIdToken();
    }
}
```



说明：

- `getIDToken` 方法返回了Keycloak security context，其中包含当前登录的Keycloak User的详细信息。



## 小结



本文的完整代码示例：

- [springboot-keycloak-demo](https://github.com/cookcodeblog/springboot-keycloak-demo)



## 参考文档

- [A Quick Guide to Using Keycloak with Spring Boot](https://www.baeldung.com/spring-boot-keycloak)
- <https://github.com/eugenp/tutorials/tree/master/spring-boot-modules/spring-boot-keycloak>
- [Spring Security and Keycloak to Secure a Spring Boot Application - A First Look](https://www.thomasvitale.com/spring-security-keycloak/)
- <https://github.com/ThomasVitale/spring-keycloak-tutorials/tree/master/keycloak-spring-security-first-look>
- [Easily secure your Spring Boot applications with Keycloak](https://developers.redhat.com/blog/2017/05/25/easily-secure-your-spring-boot-applications-with-keycloak)


