package com.onpu.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private String uniqueId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private String registerAndGetToken(String loginId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"loginId": "%s", "password": "password123", "nickname": "テスト"}
                    """.formatted(loginId)))
            .andExpect(status().isOk())
            .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }

    @Test
    void register_withValidRequest_returns200() throws Exception {
        String id = uniqueId();
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"loginId": "%s", "password": "password123", "nickname": "テスト"}
                    """.formatted(id)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.nickname").value("テスト"));
    }

    @Test
    void register_withBlankLoginId_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"loginId": "", "password": "password123", "nickname": "テスト"}
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    void register_withShortPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"loginId": "%s", "password": "abc", "nickname": "テスト"}
                    """.formatted(uniqueId())))
            .andExpect(status().isBadRequest());
    }

    @Test
    void login_withWrongPassword_returns400() throws Exception {
        String id = uniqueId();
        registerAndGetToken(id);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"loginId": "%s", "password": "wrongpassword"}
                    """.formatted(id)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void login_withCorrectPassword_returns200() throws Exception {
        String id = uniqueId();
        registerAndGetToken(id);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"loginId": "%s", "password": "password123"}
                    """.formatted(id)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void me_withValidToken_returns200() throws Exception {
        String id = uniqueId();
        String token = registerAndGetToken(id);

        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.loginId").value(id));
    }

    @Test
    void me_withoutToken_returnsError() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
            .andExpect(status().isForbidden());
    }
}
