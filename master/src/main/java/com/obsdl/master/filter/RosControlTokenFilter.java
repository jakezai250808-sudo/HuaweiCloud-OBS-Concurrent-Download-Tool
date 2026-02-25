package com.obsdl.master.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.obsdl.master.api.ApiResponse;
import com.obsdl.master.service.ApiTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RosControlTokenFilter extends OncePerRequestFilter {

    private static final String TOKEN_HEADER = "X-CTRL-TOKEN";

    private final ApiTokenService apiTokenService;
    private final ObjectMapper objectMapper;

    public RosControlTokenFilter(ApiTokenService apiTokenService, ObjectMapper objectMapper) {
        this.apiTokenService = apiTokenService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/v1/ros");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = request.getHeader(TOKEN_HEADER);
        if (!apiTokenService.isTokenValid(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error(40101, "Unauthorized token")));
            return;
        }
        filterChain.doFilter(request, response);
    }
}
