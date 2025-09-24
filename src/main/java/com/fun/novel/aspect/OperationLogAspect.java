package com.fun.novel.aspect;

import com.alibaba.fastjson.JSON;
import com.fun.novel.annotation.OperationLog;
import com.fun.novel.entity.UserOpLog;
import com.fun.novel.enums.OpType;
import com.fun.novel.service.UserOpLogService;
import com.fun.novel.service.UserService;
import com.fun.novel.utils.JwtUtil;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;

/**
 * 操作日志记录切面
 */
@Aspect
@Component
public class OperationLogAspect {
    
    private static final Logger log = LoggerFactory.getLogger(OperationLogAspect.class);
    
    private final UserOpLogService userOpLogService;
    
    private final UserService userService;
    
    private final JwtUtil jwtUtil;
    
    public OperationLogAspect(UserOpLogService userOpLogService, @Lazy UserService userService, JwtUtil jwtUtil) {
        this.userOpLogService = userOpLogService;
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }
    
    /**
     * 配置织入点
     */
    @Pointcut("@annotation(com.fun.novel.annotation.OperationLog)")
    public void logPointCut() {
    }
    
    /**
     * 处理完请求后执行
     *
     * @param joinPoint 切点
     */
    @AfterReturning(pointcut = "logPointCut()", returning = "jsonResult")
    public void doAfterReturning(JoinPoint joinPoint, Object jsonResult) {
        handleLog(joinPoint, null, jsonResult);
    }
    
    /**
     * 拦截异常操作
     *
     * @param joinPoint 切点
     * @param e         异常
     */
    @AfterThrowing(value = "logPointCut()", throwing = "e")
    public void doAfterThrowing(JoinPoint joinPoint, Exception e) {
        handleLog(joinPoint, e, null);
    }
    
    protected void handleLog(final JoinPoint joinPoint, final Exception e, Object jsonResult) {
        try {
            // 获得注解
            OperationLog controllerLog = getAnnotationLog(joinPoint);
            if (controllerLog == null) {
                return;
            }
            
            // 获取当前的请求对象
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return;
            }
            
            HttpServletRequest request = attributes.getRequest();
            
            // 创建操作日志对象
            UserOpLog userOpLog = new UserOpLog();
            userOpLog.setOpStatus(e == null ? 1 : 0); // 1表示成功，0表示失败
            userOpLog.setOpType(controllerLog.opType());
            userOpLog.setOpName(controllerLog.opName());
            userOpLog.setMethodName(joinPoint.getTarget().getClass().getName() + "." + joinPoint.getSignature().getName());
            userOpLog.setRequestType(request.getMethod());
            userOpLog.setRequestUrl(request.getRequestURI());
            userOpLog.setRequestIp(getIpAddress(request));
            
            // 设置用户信息
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                try {
                    token = token.substring(7);
                    String username = jwtUtil.getUsernameFromToken(token);
                    userOpLog.setUserName(username);
                    // 根据用户名获取用户ID
                    Long userId = userService.getUserIdByUsername(username);
                    userOpLog.setUserId(userId != null ? userId : 0L);
                } catch (Exception ex) {
                    log.error("解析JWT Token失败: {}", ex.getMessage());
                    userOpLog.setUserId(0L);
                }
            } else {
                userOpLog.setUserId(0L);
            }
            
            // 设置请求参数
            // 如果注解中明确指定要记录参数，或者操作类型不是查询或导出操作，则记录参数
            if (controllerLog.recordParams() || 
                (controllerLog.opType() != OpType.QUERY_CODE && controllerLog.opType() != OpType.EXPORT_CODE)) {
                userOpLog.setRequestParams(argsArrayToString(joinPoint.getArgs()));
            }
            
            // 设置返回结果并处理长度限制
            String responseResult = JSON.toJSONString(jsonResult);
            // 限制response_result字段长度，防止超过数据库字段限制
            if (responseResult != null && responseResult.length() > 2000) {
                responseResult = responseResult.substring(0, 2000);
            }
            userOpLog.setResponseResult(responseResult);
            
            // 保存数据库
            userOpLogService.saveOpLog(userOpLog);
        } catch (Exception exp) {
            // 记录本地异常日志
            log.error("==前置通知异常==");
            log.error("异常信息:{}", exp.getMessage());
            exp.printStackTrace();
        }
    }
    
    /**
     * 是否存在注解，如果存在就获取
     */
    private OperationLog getAnnotationLog(JoinPoint joinPoint) {
        Signature signature = joinPoint.getSignature();
        MethodSignature methodSignature = (MethodSignature) signature;
        Method method = methodSignature.getMethod();
        
        if (method != null) {
            return method.getAnnotation(OperationLog.class);
        }
        return null;
    }
    
    /**
     * 获取IP地址
     */
    public String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
    
    /**
     * 参数拼装
     */
    private String argsArrayToString(Object[] paramsArray) {
        StringBuilder params = new StringBuilder();
        if (paramsArray != null && paramsArray.length > 0) {
            for (Object o : paramsArray) {
                if (o != null && !isFilterObject(o)) {
                    try {
                        Object jsonObj = JSON.toJSON(o);
                        params.append(jsonObj.toString()).append(" ");
                    } catch (Exception e) {
                        // 如果转换失败，则使用toString方法
                        params.append(o.toString()).append(" ");
                    }
                }
            }
        }
        return params.toString().trim();
    }
    
    /**
     * 判断是否需要过滤的对象。
     *
     * @param o 对象信息。
     * @return 如果是需要过滤的对象，则返回true；否则返回false。
     */
    @SuppressWarnings("rawtypes")
    public boolean isFilterObject(final Object o) {
        Class<?> clazz = o.getClass();
        if (clazz.isArray()) {
            return clazz.getComponentType().isAssignableFrom(MultipartFile.class);
        } else if (Collection.class.isAssignableFrom(clazz)) {
            Collection collection = (Collection) o;
            for (Object value : collection) {
                return value instanceof MultipartFile;
            }
        } else if (MultipartFile.class.isAssignableFrom(clazz)) {
            return true;
        } else if (HttpServletRequest.class.isAssignableFrom(clazz)) {
            return true;
        } else if (HttpServletResponse.class.isAssignableFrom(clazz)) {
            return true;
        }
        return o instanceof MultipartFile || o instanceof HttpServletRequest || o instanceof HttpServletResponse;
    }
}