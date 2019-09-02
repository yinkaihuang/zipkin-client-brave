package cn.bucheng.trace.interceptor;

import brave.Tracer;
import brave.Tracing;
import com.github.kristofa.brave.Brave;
import com.github.kristofa.brave.ClientTracer;
import com.github.kristofa.brave.Sampler;
import com.github.kristofa.brave.SpanId;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import zipkin.Span;
import zipkin.reporter.Reporter;

/**
 * @author ：yinchong
 * @create ：2019/9/2 15:23
 * @description：
 * @modified By：
 * @version:
 */
@Aspect
@Component
@Order(-Integer.MAX_VALUE)
@Slf4j
public class ZipKinInterceptor {

    private Reporter<Span> reporter;

    public ZipKinInterceptor(Reporter<Span> reporter) {
        this.reporter = reporter;
    }

    @Around("@annotation(cn.bucheng.trace.annotation.Zipkin)")
    public Object aroundZipKin(ProceedingJoinPoint point) {
        String simpleClassName = point.getTarget().getClass().getSimpleName();
        String className = point.getTarget().getClass().getName();
        String methodName = ((MethodSignature) point.getSignature()).getMethod().getName();
        Object[] args = point.getArgs();
        SpanId spanId = null;
        Brave brave = null;
        ClientTracer clientTracer = null;
        try {
            Brave.Builder builder = new Brave.Builder(simpleClassName);
            builder.reporter(reporter);
            builder.traceSampler(Sampler.ALWAYS_SAMPLE);
            brave = builder.build();
            //获取跟踪链路
            clientTracer = brave.clientTracer();
            Tracer tracer = Tracing.currentTracer();
            spanId = clientTracer.startNewSpan(methodName);
            if (tracer != null) {
                long traceId = tracer.currentSpan().context().traceId();
                long parentId = tracer.currentSpan().context().parentId();
                spanId = SpanId.builder().parentId(parentId).spanId(spanId.spanId).traceId(traceId).build();
            }
            //记录当前开始时间
            clientTracer.setClientSent();
            //设置其他属性的key和value
            clientTracer.submitBinaryAnnotation("class", className);
            clientTracer.submitBinaryAnnotation("method", methodName);
            clientTracer.submitBinaryAnnotation("args", argsToString(args));
            return point.proceed();
        } catch (Throwable throwable) {
            log.error(throwable.toString());
            throw new RuntimeException(throwable.toString());
        } finally {
            //记录结束开始时间
            clientTracer.setClientReceived();
            //将当前的spandId设置到链路中构成树形结构，这里内部是static的ThreadLocal变量
            brave.serverTracer().setStateCurrentTrace(spanId, simpleClassName);
        }
    }

    private String argsToString(Object[] args) {
        if (args == null || args.length == 0)
            return "";
        StringBuilder sb = new StringBuilder("[");
        int len = args.length;
        for (int i = 0; i < len; i++) {
            sb.append(args[i].toString());
            if (i == len - 1) {
                sb.append("]");
            } else {
                sb.append(",");
            }
        }
        return sb.toString();
    }
}
