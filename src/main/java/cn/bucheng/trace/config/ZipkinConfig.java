package cn.bucheng.trace.config;


import cn.bucheng.trace.interceptor.ZipKinInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import zipkin.Span;
import zipkin.reporter.AsyncReporter;
import zipkin.reporter.Reporter;
import zipkin.reporter.Sender;
import zipkin.reporter.okhttp3.OkHttpSender;

import java.util.concurrent.TimeUnit;

/**
 * @author ：yinchong
 * @create ：2019/9/2 15:11
 * @description：
 * @modified By：
 * @version:
 */
@Configuration
@Slf4j
public class ZipkinConfig {


    @Bean
    public Reporter<Span> braveReporter(@Value("${spring.zipkin.base-url}") String zipkinUrl) {
        String url = zipkinUrl + "/api/v1/spans";
        log.info("create brave reporter url:{}", url);
        Sender sender = OkHttpSender.create(url);
        return AsyncReporter.builder(sender)
                .messageTimeout(500, TimeUnit.MILLISECONDS)
                .build();
    }

    @Bean
    public ZipKinInterceptor zipKinInterceptor(Reporter<Span> braveReporter) {
        return new ZipKinInterceptor(braveReporter);
    }
}
