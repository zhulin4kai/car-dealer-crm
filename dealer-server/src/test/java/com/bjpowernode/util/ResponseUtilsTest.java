package com.bjpowernode.util;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import static org.junit.jupiter.api.Assertions.*;

class ResponseUtilsTest {

    @Test
    void testWriteShouldSetContentType() throws UnsupportedEncodingException {
        MockHttpServletResponse response = new MockHttpServletResponse();

        ResponseUtils.write(response, "{\"code\":200}");

        assertTrue(response.getContentType().contains("application/json"));
        assertTrue(response.getContentType().contains("UTF-8"));
    }

    @Test
    void testWriteShouldWriteContent() throws UnsupportedEncodingException {
        MockHttpServletResponse response = new MockHttpServletResponse();
        String expectedResult = "{\"code\":200,\"msg\":\"success\"}";

        ResponseUtils.write(response, expectedResult);

        assertEquals(expectedResult, response.getContentAsString());
    }

    @Test
    void testWriteWithEmptyString() throws UnsupportedEncodingException {
        MockHttpServletResponse response = new MockHttpServletResponse();

        ResponseUtils.write(response, "");

        assertEquals("", response.getContentAsString());
    }

    @Test
    void testWriteWithSpecialCharacters() throws UnsupportedEncodingException {
        MockHttpServletResponse response = new MockHttpServletResponse();
        String expectedResult = "{\"msg\":\"中文消息\"}";

        ResponseUtils.write(response, expectedResult);

        assertEquals(expectedResult, response.getContentAsString());
    }
}
