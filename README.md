# 增强zipkin让其能够追踪server和mapper等内部调用方法

```
获取当前链路的TraceId
Tracer tracer = Tracing.currentTracer();
```