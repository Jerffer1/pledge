package cn.pledge.envconsole;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.net.InetAddress;
import java.net.UnknownHostException;

@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class})
@Slf4j
public class EnvConsoleApplication {

  public static void main(String[] args) throws UnknownHostException {

  ConfigurableApplicationContext application = SpringApplication.run(EnvConsoleApplication.class, args);

  Environment env = application.getEnvironment();
  String ip = InetAddress.getLocalHost().getHostAddress();
  String port = env.getProperty("server.port");
  //    String path = String.valueOf(env.getProperty("server.servlet.context-path"));
  String path = "";
        if ("null".equals(path)) { path = "";}
        log.info("\n----------------------------------------------------------\n\t" +
                "Application culture is running! Access URLs:\n\t" +
                "Local: \t\thttp://localhost:" + port + path + "/doc.html\n\t" +
          "External: \thttp://" + ip + ":" + port + path + "/\n\t" +
          "Swagger文档: \thttp://" + ip + ":" + port + path + "/doc.html\n" +
          "----------------------------------------------------------");
}

}
