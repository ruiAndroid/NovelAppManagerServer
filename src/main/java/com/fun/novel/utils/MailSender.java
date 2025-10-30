package com.fun.novel.utils;

import com.fun.novel.entity.UserOpLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import jakarta.mail.*;
import jakarta.mail.internet.*;

import java.time.format.DateTimeFormatter;
import java.util.Properties;

/**
 * 邮件发送工具类
 */
@Component
public class MailSender {
    
    private static final Logger log = LoggerFactory.getLogger(MailSender.class);
    
    // 发送邮件的账号
    private static final String FROM_EMAIL = "jianrui@fun.tv";
    // 邮件接收方（可配置多个接收方）
    private static final String[] TO_EMAILS = {"jianrui@fun.tv","wangna@fun.tv","wangjr@fun.tv","pengyx@fun.tv"};
    // 发送邮件的密码
    // 注意：对于腾讯企业邮箱，可能需要使用应用专用密码而不是登录密码
    // 如果密码包含特殊字符，确保正确转义
    private static final String FROM_PASSWORD = "Ss123456!";
    // 邮件服务器SMTP地址
    private static final String SMTP_HOST = "smtp.exmail.qq.com";
    // 邮件服务器SMTP端口 - 腾讯企业邮箱建议使用465（SMTPS）或587（SMTP+STARTTLS）
    private static final String SMTP_PORT = "465";
    
    /**
     * 异步发送敏感操作告警邮件
     * @param userOpLog 操作日志信息
     */
    @Async
    public void sendSensitiveOperationAlertAsync(UserOpLog userOpLog) {
        // 异步执行邮件发送，不影响主流程
        sendSensitiveOperationAlert(userOpLog);
    }
    
    /**
     * 发送敏感操作告警邮件
     * @param userOpLog 操作日志信息
     */
    public void sendSensitiveOperationAlert(UserOpLog userOpLog) {
        // 格式化操作时间
        String operationTime = userOpLog.getUpdateTime() != null ? 
            userOpLog.getUpdateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : 
            "未知时间";
        
        // 构建邮件内容 - 使用HTML格式以兼容参考代码
        StringBuilder content = new StringBuilder();
        content.append("<html><body>")
                .append("<span style='color:red;'>") // 用户名标红
                .append(userOpLog.getUserName())
                .append("</span>")
                .append("&nbsp;") // 添加空格
                .append("(")
                .append(userOpLog.getUserId())
                .append(")")
                .append("&nbsp;&nbsp;") // 添加更多空格
                .append(operationTime)
                .append("<br>") // 换行
                .append("操作了")
                .append("<br>") // 换行
                .append("<span style='color:red;'>") // 操作名称标红
                .append(userOpLog.getOpName())
                .append("</span>")
                .append("<br>") // 换行
                .append(userOpLog.getRequestUrl())
                .append("<br>") // 换行
                .append("请求参数: ")
                .append("<pre>") // 使用<pre>标签格式化JSON
                .append(formatJson(userOpLog.getRequestParams()))
                .append("</pre>")
                .append("</body></html>");
        
        // 添加重试机制，使用指数退避策略
        int maxRetries = 3;
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.info("尝试发送邮件，第{}次，发送方: {}, 接收方: {}", attempt, FROM_EMAIL, String.join(",", TO_EMAILS));
                
                // 创建邮件会话
                Session session = createMailSession();
                
                // 创建邮件消息，使用参考代码中的方式
                MimeMessage message = new MimeMessage(session);
                message.setFrom(new InternetAddress(FROM_EMAIL));
                
                // 设置接收者地址数组
                InternetAddress[] addresses = new InternetAddress[TO_EMAILS.length];
                for(int i = 0; i < TO_EMAILS.length; i++) {
                    addresses[i] = new InternetAddress(TO_EMAILS[i]);
                }
                message.setRecipients(Message.RecipientType.TO, addresses);
                
                // 设置主题和内容
                message.setSubject("文曲-敏感操作告警");
                
                // 使用HTML格式设置内容，与参考代码一致
                message.setContent(content.toString(), "text/html;charset=utf-8");

                // 使用显式的Transport连接方式，更好地控制连接过程
                Transport transport = null;
                try {
                    // 获取SMTPS传输
                    transport = session.getTransport("smtps");
                    
                    // 显式连接SMTP服务器
                    log.debug("正在连接SMTP服务器: {}, 端口: {}", SMTP_HOST, SMTP_PORT);
                    transport.connect(SMTP_HOST, FROM_EMAIL, FROM_PASSWORD);
                    log.debug("SMTP服务器连接成功");
                    
                    // 发送邮件
                    transport.sendMessage(message, message.getAllRecipients());
                    
                    log.info("敏感操作告警邮件发送成功: 用户ID={}, 用户名={}, 操作类型={}, 操作名称={}, 请求URL={}, 操作时间={}, 发送方={}, 接收方={}",
                            userOpLog.getUserId(), userOpLog.getUserName(), userOpLog.getOpType(),
                            userOpLog.getOpName(), userOpLog.getRequestUrl(), operationTime, FROM_EMAIL, String.join(",", TO_EMAILS));
                    return; // 发送成功，直接返回
                } finally {
                    // 确保关闭连接
                    if (transport != null && transport.isConnected()) {
                        try {
                            transport.close();
                            log.debug("SMTP连接已关闭");
                        } catch (Exception e) {
                            log.warn("关闭SMTP连接时出错", e);
                        }
                    }
                }
            } catch (AuthenticationFailedException e) {
                // 认证失败，可能是系统繁忙、密码错误、安全限制或IP限制
                log.error("SMTP认证失败(第{}次尝试)，错误: {}", attempt, e.getMessage(), e);
                log.warn("请确认：1. 邮箱密码是否正确，腾讯企业邮箱可能需要使用应用专用密码而非登录密码 2. 确认账号是否启用了SMTP服务 3. 检查是否存在IP限制或频率限制");
                log.warn("建议：尝试在浏览器中登录腾讯企业邮箱，在设置中开启SMTP服务并生成应用专用密码");
                if (attempt < maxRetries) {
                    try {
                        // 使用指数退避策略，每次重试时间增加
                        int retryIntervalMs = 2000 * attempt;
                        log.info("{}秒后重试...", retryIntervalMs / 1000);
                        Thread.sleep(retryIntervalMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("邮件发送重试被中断", ie);
                        break;
                    }
                }
            } catch (MessagingException e) {
                // 其他邮件发送错误
                log.error("发送敏感操作告警邮件失败(第{}次尝试)，发送方: {}, 接收方: {}, 错误信息: {}", 
                        attempt, FROM_EMAIL, String.join(",", TO_EMAILS), e.getMessage(), e);
                if (attempt < maxRetries) {
                    try {
                        int retryIntervalMs = 2000 * attempt;
                        Thread.sleep(retryIntervalMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("邮件发送重试被中断", ie);
                        break;
                    }
                }
            } catch (Exception e) {
                // 未预期的错误
                log.error("发送敏感操作告警邮件过程中发生未预期错误(第{}次尝试): {}", attempt, e.getMessage(), e);
                break; // 严重错误，不继续重试
            }
        }
        
        // 所有重试都失败了
        log.error("邮件发送失败: 所有{}次尝试都失败，可能需要检查邮箱配置或稍后再试", maxRetries);
    }
    
    /**
     * 创建邮件会话
     * @return Session 邮件会话实例
     */
    private static Session createMailSession() {
        Properties props = new Properties();
        
        // 对于腾讯企业邮箱，使用SMTPS协议而不是SMTP，因为端口是465
        props.setProperty("mail.transport.protocol", "smtps");
        props.setProperty("mail.smtps.host", SMTP_HOST);
        props.setProperty("mail.smtps.port", SMTP_PORT);
        props.setProperty("mail.smtps.auth", "true");
//        props.put("mail.debug", "true"); // 启用调试模式，帮助诊断问题
        
        // 对于腾讯企业邮箱，显式指定SSL/TLS配置
        props.put("mail.smtps.ssl.enable", "true");
        props.put("mail.smtps.ssl.checkserveridentity", "false"); // 忽略服务器身份验证，可能有助于绕过某些安全限制
        props.put("mail.smtps.ssl.trust", SMTP_HOST); // 信任指定的SMTP主机
        
        // 设置超时参数
        props.put("mail.smtps.connectiontimeout", "30000");
        props.put("mail.smtps.timeout", "30000");
        
        // 显式指定认证方式
        props.put("mail.smtps.auth.mechanisms", "LOGIN PLAIN");
        
        // 记录SMTP配置，便于调试
//        log.info("SMTPS配置：host={}, port={}, fromEmail={}", SMTP_HOST, SMTP_PORT, FROM_EMAIL);
        
        // 创建认证器
        final String user = FROM_EMAIL;
        final String pwd = FROM_PASSWORD;
        
        // 添加详细的认证调试信息
        log.debug("正在配置SMTP认证，用户名长度: {}, 密码长度: {}", 
                 user != null ? user.length() : 0, 
                 pwd != null ? pwd.length() : 0);
        
        Authenticator authenticator = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                // 这是用于调试的打印，实际生产环境应该移除或注释掉
                log.debug("尝试使用用户名 '{}' 进行SMTP认证", user);
                return new PasswordAuthentication(user, pwd);
            }
        };
        
        return Session.getInstance(props, authenticator);
    }
    
    /**
     * 格式化JSON字符串，添加适当的缩进和换行以提高可读性
     * @param jsonStr 原始JSON字符串
     * @return 格式化后的JSON字符串
     */
    private String formatJson(String jsonStr) {
        if (jsonStr == null || jsonStr.isEmpty()) {
            return "";
        }
        
        try {
            // 简单的JSON格式化实现
            StringBuilder formatted = new StringBuilder();
            int indent = 0;
            boolean inQuote = false;
            char lastChar = 0;
            
            for (char c : jsonStr.toCharArray()) {
                switch (c) {
                    case '"':
                        if (lastChar != '\\') {
                            inQuote = !inQuote;
                        }
                        formatted.append(c);
                        break;
                    case '{':
                    case '[':
                        formatted.append(c);
                        if (!inQuote) {
                            indent++;
                            formatted.append("\n").append("  ".repeat(indent));
                        }
                        break;
                    case '}':
                    case ']':
                        if (!inQuote) {
                            indent--;
                            formatted.append("\n").append("  ".repeat(indent));
                        }
                        formatted.append(c);
                        break;
                    case ',':
                        formatted.append(c);
                        if (!inQuote) {
                            formatted.append("\n").append("  ".repeat(indent));
                        }
                        break;
                    case ':':
                        formatted.append(c);
                        if (!inQuote) {
                            formatted.append(" ");
                        }
                        break;
                    default:
                        formatted.append(c);
                        break;
                }
                lastChar = c;
            }
            
            return formatted.toString();
        } catch (Exception e) {
            // 如果格式化失败，返回原始字符串
            return jsonStr;
        }
    }
}