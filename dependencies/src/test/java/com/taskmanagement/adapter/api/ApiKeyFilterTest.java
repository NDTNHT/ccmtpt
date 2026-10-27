package com.taskmanagement.adapter.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("API key filter")
class ApiKeyFilterTest {

    @Test
    @DisplayName("Authentication is disabled when no key is configured")
    void shouldPassThroughWhenKeyIsNotConfigured() throws Exception {
        ApiKeyFilter filter = new ApiKeyFilter("");
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request("/api/tasks"), response, chain);

        assertNotNull(chain.getRequest());
        assertEquals(200, response.getStatus());
    }

    @Test
    @DisplayName("A request without the key is rejected")
    void shouldRejectMissingKey() throws Exception {
        ApiKeyFilter filter = new ApiKeyFilter("secret");
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request("/api/tasks"), response, chain);

        assertNull(chain.getRequest());
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("A request with a wrong key is rejected")
    void shouldRejectWrongKey() throws Exception {
        ApiKeyFilter filter = new ApiKeyFilter("secret");
        MockHttpServletRequest request = request("/api/tasks");
        request.addHeader(ApiKeyFilter.API_KEY_HEADER, "wrong");
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertNull(chain.getRequest());
        assertEquals(401, response.getStatus());
    }

    @Test
    @DisplayName("A request with the correct key is accepted")
    void shouldAcceptCorrectKey() throws Exception {
        ApiKeyFilter filter = new ApiKeyFilter("secret");
        MockHttpServletRequest request = request("/api/tasks");
        request.addHeader(ApiKeyFilter.API_KEY_HEADER, "secret");
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertNotNull(chain.getRequest());
        assertEquals(200, response.getStatus());
    }

    @Test
    @DisplayName("Health probes stay public when authentication is enabled")
    void shouldAllowHealthProbeWithoutKey() throws Exception {
        ApiKeyFilter filter = new ApiKeyFilter("secret");
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request("/actuator/health"), response, chain);

        assertNotNull(chain.getRequest());
        assertEquals(200, response.getStatus());
    }

    private MockHttpServletRequest request(String uri) {
        return new MockHttpServletRequest("GET", uri);
    }
}
