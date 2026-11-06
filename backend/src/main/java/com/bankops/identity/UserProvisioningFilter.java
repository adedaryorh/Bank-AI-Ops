package com.bankops.identity;

import com.bankops.common.UserContext;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class UserProvisioningFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(UserProvisioningFilter.class);

    private final UserProvisioningService provisioning;

    public UserProvisioningFilter(UserProvisioningService provisioning) {
        this.provisioning = provisioning;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request.getRequestURI().startsWith("/api/v1")
            && SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken jwtAuth) {
            try {
                UserContext user = provisioning.provision(jwtAuth.getToken());
                request.setAttribute(UserContext.REQUEST_ATTRIBUTE, user);
            } catch (RuntimeException ex) {
                log.error("User provisioning failed for subject {}", jwtAuth.getToken().getSubject(), ex);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType("application/problem+json");
                response.getWriter().write("""
                    {"type":"about:blank","title":"Provisioning failed","status":500,"detail":"The account could not be provisioned."}""");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
