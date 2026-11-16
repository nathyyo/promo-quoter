package com.promoquoter.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Base class for HTTP-level integration tests: full application context, real H2 database, MockMvc.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(DatabaseCleaner.class)
public abstract class IntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    private DatabaseCleaner databaseCleaner;

    @BeforeEach
    void cleanDatabase() {
        databaseCleaner.clean();
    }

    protected ResultActions postJson(String path, Object body) throws Exception {
        String json = body instanceof String raw ? raw : objectMapper.writeValueAsString(body);
        return mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(json));
    }
}
