package com.fun.novel.ai.controller;

import com.fun.novel.ai.entity.FunAiApp;
import com.fun.novel.ai.enums.FunAiAppStatus;
import com.fun.novel.service.FunAiAppService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * FunAI 应用站点访问（方式B：由 Spring Boot 提供静态资源）
 *
 * 访问规则：
 * - /fun-ai-app/{userId}/{appId}/            -> dist/index.html
 * - /fun-ai-app/{userId}/{appId}/assets/...  -> dist 下对应文件
 * - SPA 路由：如果资源不存在，则回退到 index.html
 */
@Controller
public class FunAiAppSiteController {

    private static final Logger log = LoggerFactory.getLogger(FunAiAppSiteController.class);

    private final FunAiAppService funAiAppService;

    // 与 FunAiAppServiceImpl 保持一致：用户应用根目录
    private final String userPath;

    public FunAiAppSiteController(FunAiAppService funAiAppService,
                                  @org.springframework.beans.factory.annotation.Value("${funai.userPath}") String userPath) {
        this.funAiAppService = funAiAppService;
        this.userPath = userPath;
    }

    @GetMapping({"/fun-ai-app/{userId}/{appId}", "/fun-ai-app/{userId}/{appId}/", "/fun-ai-app/{userId}/{appId}/**"})
    public ResponseEntity<Resource> serve(
            @PathVariable Long userId,
            @PathVariable Long appId,
            HttpServletRequest request
    ) {
        try {
            // 0) 规范化URL：不带尾随 / 的入口路径统一 302 到带 /
            // 例如 /fun-ai-app/1/100 -> /fun-ai-app/1/100/
            String uri = request.getRequestURI();
            String prefix = "/fun-ai-app/" + userId + "/" + appId;
            if (prefix.equals(uri)) {
                return ResponseEntity.status(HttpStatus.FOUND)
                        .header(HttpHeaders.LOCATION, prefix + "/")
                        .build();
            }

            // 1) 校验应用归属
            FunAiApp app = funAiAppService.getAppByIdAndUserId(appId, userId);
            if (app == null) {
                return ResponseEntity.notFound().build();
            }
            // 仅 READY（可访问）状态才允许访问站点资源
            if (app.getAppStatus() == null || app.getAppStatus() != FunAiAppStatus.READY.code()) {
                return ResponseEntity.notFound().build();
            }

            String basePath = sanitizeUserPath(userPath);
            if (basePath == null || basePath.isEmpty()) {
                return ResponseEntity.internalServerError().build();
            }

            // 2) 计算 dist 目录：{userPath}/{userId}/{sanitize(appName)}/deploy/{root}/dist
            Path appDir = Paths.get(basePath, String.valueOf(userId), sanitizeFileName(app.getAppName()));
            Path deployDir = appDir.resolve("deploy");
            Path projectRoot = detectProjectRoot(deployDir);
            Path distDir = projectRoot.resolve("dist");

            if (Files.notExists(distDir)) {
                // build 未完成 或 build 失败
                return ResponseEntity.notFound().build();
            }

            // 3) 解析请求路径中 dist 下的相对路径
            uri = request.getRequestURI(); // e.g. /fun-ai-app/1/100/assets/index-xxx.js
            String rel = uri.startsWith(prefix) ? uri.substring(prefix.length()) : "/";
            if (rel.isEmpty() || "/".equals(rel)) {
                rel = "/index.html";
            }
            if (rel.startsWith("/")) {
                rel = rel.substring(1);
            }

            Path target = distDir.resolve(rel).normalize();
            if (!target.startsWith(distDir)) {
                return ResponseEntity.badRequest().build();
            }

            // 4) SPA 路由回退：文件不存在则返回 index.html
            if (Files.notExists(target) || Files.isDirectory(target)) {
                target = distDir.resolve("index.html");
            }

            if (Files.notExists(target)) {
                return ResponseEntity.notFound().build();
            }

            FileSystemResource resource = new FileSystemResource(target.toFile());
            MediaType mediaType = MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM);

            // 禁止缓存（你如果想缓存可改为 max-age）
            return ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                    .contentType(mediaType)
                    .body(resource);
        } catch (Exception e) {
            log.error("serve fun-ai-app failed: userId={}, appId={}, error={}", userId, appId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private String sanitizeUserPath(String p) {
        if (p == null) return null;
        return p.trim().replaceAll("^[\"']|[\"']$", "");
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null) return "unnamed";
        return fileName.replaceAll("[<>:\"/\\\\|?*]", "_");
    }

    private Path detectProjectRoot(Path deployDir) {
        try {
            if (Files.exists(deployDir.resolve("package.json"))) {
                return deployDir;
            }
            var children = Files.list(deployDir).filter(Files::isDirectory).toList();
            if (children.size() == 1) {
                Path only = children.get(0);
                if (Files.exists(only.resolve("package.json"))) {
                    return only;
                }
            }
        } catch (Exception ignore) {
        }
        return deployDir;
    }
}


