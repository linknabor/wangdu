package com.yumu.hexie.common.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {SecurityAutoConfiguration.class })
@Configuration
@ComponentScan(basePackages = {"com.yumu.hexie"}, includeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = {"com.yumu.hexie.web.*","com.yumu.hexie.service.*"}))
@EnableJpaRepositories({"com.yumu.hexie.model.*"})
@EnableScheduling
@EnableAspectJAutoProxy
@EnableAsync
@EnableCaching(proxyTargetClass=true)
public class AppConfig {

    public static void main(String[] args) {
        SpringApplication.run(AppConfig.class, args);
    }
    @Bean
    public EmbeddedServletContainerFactory EmbeddedServletContainerFactory(){
        TomcatEmbeddedServletContainerFactory factory = new TomcatEmbeddedServletContainerFactory();
        factory.setPort(8890);
        factory.addConnectorCustomizers(new AppTomcatConnectorCustomizer());
        return factory;
    }
    
}
