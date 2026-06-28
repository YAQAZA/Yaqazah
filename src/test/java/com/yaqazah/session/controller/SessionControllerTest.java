package com.yaqazah.session.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yaqazah.session.dto.LogPayload;
import com.yaqazah.session.dto.SessionPayload;
import com.yaqazah.session.dto.SessionUploadRequest;
import com.yaqazah.session.model.Session;
import com.yaqazah.session.service.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SessionControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private SessionService sessionService;

    @InjectMocks
    private SessionController sessionController;

    private User mockDriverUser;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        mockDriverUser = new User(
                "driver@yaqazah.com",
                "",
                List.of(new SimpleGrantedAuthority("ROLE_FLEET_DRIVER"))
        );

        // Configure standalone MockMvc with a custom resolver for @AuthenticationPrincipal
        this.mockMvc = MockMvcBuilders.standaloneSetup(sessionController)
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                        return mockDriverUser;
                    }
                })
                .build();
    }

    @Test
    public void uploadSession_Success() throws Exception {
        UUID sessionId = UUID.randomUUID();
        Session mockSession = new Session();
        mockSession.setSessionId(sessionId);

        SessionPayload sessionPayload = new SessionPayload(null, "2026-06-27T03:00:00Z", "2026-06-27T04:00:00Z", 1.0);
        LogPayload logPayload = new LogPayload("Distraction", "Driver looking away", "2026-06-27T03:15:00Z", 1, 2, "http://snapshot", "base64");
        SessionUploadRequest request = new SessionUploadRequest(sessionPayload, List.of(logPayload));

        when(sessionService.uploadSession(any(SessionUploadRequest.class), eq("driver@yaqazah.com")))
                .thenReturn(mockSession);

        mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Session uploaded successfully. ID: " + sessionId));
    }

    @Test
    public void uploadSession_Forbidden_AnotherDriver() throws Exception {
        UUID targetUserId = UUID.randomUUID();
        SessionPayload sessionPayload = new SessionPayload(targetUserId, "2026-06-27T03:00:00Z", "2026-06-27T04:00:00Z", 1.0);
        SessionUploadRequest request = new SessionUploadRequest(sessionPayload, List.of());

        when(sessionService.uploadSession(any(SessionUploadRequest.class), eq("driver@yaqazah.com")))
                .thenThrow(new SecurityException("Access denied: You cannot upload sessions for another driver."));

        mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(content().string("Error: Access denied: You cannot upload sessions for another driver."));
    }

    @Test
    public void uploadSession_BadRequest_Exception() throws Exception {
        SessionPayload sessionPayload = new SessionPayload(null, "2026-06-27T03:00:00Z", "2026-06-27T04:00:00Z", 1.0);
        SessionUploadRequest request = new SessionUploadRequest(sessionPayload, List.of());

        when(sessionService.uploadSession(any(SessionUploadRequest.class), eq("driver@yaqazah.com")))
                .thenThrow(new RuntimeException("Something went wrong"));

        mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Error: Something went wrong"));
    }
}
