package com.zjgsu.cxt.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class InstanceHeaderFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(InstanceHeaderFilter.class);

    private final String instanceId;

    public InstanceHeaderFilter(@Value("${instance.id:local}") String instanceId) {
        this.instanceId = instanceId;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        response.setHeader("X-Instance-Id", instanceId);
        if (request.getRequestURI().startsWith("/api/")) {
            log.info("instance={} {} {}", instanceId, request.getMethod(), request.getRequestURI());
        }
        filterChain.doFilter(request, response);
    }
}
