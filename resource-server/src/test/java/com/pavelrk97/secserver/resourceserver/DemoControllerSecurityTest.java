package com.pavelrk97.secserver.resourceserver;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.opaqueToken;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DemoControllerSecurityTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void demo_withoutToken_isUnauthorized() throws Exception {
        mockMvc.perform(get("/demo"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void demo_withJwt_isOk() throws Exception {
        mockMvc.perform(get("/demo").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(content().string("Demo"));
    }

    @Test
    void demo_withOpaqueToken_isOk() throws Exception {
        mockMvc.perform(get("/demo").with(opaqueToken()))
                .andExpect(status().isOk())
                .andExpect(content().string("Demo"));
    }
}
