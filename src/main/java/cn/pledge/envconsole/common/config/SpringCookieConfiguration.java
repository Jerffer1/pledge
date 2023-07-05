package cn.pledge.envconsole.common.config;

import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Bean;
//import org.springframework.session.web.http.CookieSerializer;
//import org.springframework.session.web.http.DefaultCookieSerializer;
//
//@Service
public class SpringCookieConfiguration {
 
    private static final String cookieName = "SSO_LOGIN";
 
//    @Bean
//    public CookieSerializer loginInCookieConfig() { // 作为单例配置
//        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
//        serializer.setCookieName(cookieName); // 设置 Key
//        serializer.setCookieMaxAge(60 * 60 * 24);
//        serializer.setCookiePath("/test/");
//
//        serializer.setSameSite("NONE");  // 增加 SameSite
//        serializer.setUseSecureCookie(true); // 增加 Secure
//        return serializer;
//    }
}