package me.desair.tus.server;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class HttpMethodTest {

    @Test
    public void forName() {
        assertEquals(HttpMethod.DELETE, HttpMethod.forName("delete"));
        assertEquals(HttpMethod.GET, HttpMethod.forName("get"));
        assertEquals(HttpMethod.HEAD, HttpMethod.forName("head"));
        assertEquals(HttpMethod.PATCH, HttpMethod.forName("patch"));
        assertEquals(HttpMethod.POST, HttpMethod.forName("post"));
        assertEquals(HttpMethod.PUT, HttpMethod.forName("put"));
        assertEquals(HttpMethod.OPTIONS, HttpMethod.forName("options"));
        assertNull(HttpMethod.forName("test"));
    }

    @Test
    public void getMethodNormal() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setMethod("patch");
        assertEquals(HttpMethod.PATCH, HttpMethod.getMethodIfSupported(servletRequest, EnumSet.allOf(HttpMethod.class)));
    }

    @Test
    public void getMethodOverridden() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setMethod("post");
        servletRequest.addHeader(HttpHeader.METHOD_OVERRIDE, "patch");
        assertEquals(HttpMethod.PATCH, HttpMethod.getMethodIfSupported(servletRequest, EnumSet.allOf(HttpMethod.class)));
    }

    @Test
    public void getMethodOverriddenDoesNotExist() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setMethod("post");
        servletRequest.addHeader(HttpHeader.METHOD_OVERRIDE, "test");
        assertEquals(HttpMethod.POST, HttpMethod.getMethodIfSupported(servletRequest, EnumSet.allOf(HttpMethod.class)));
    }

    @Test
    public void getMethodNull() {
        Assertions.assertThrows(NullPointerException.class, () -> HttpMethod.getMethodIfSupported(null, EnumSet.allOf(HttpMethod.class)));
    }

    @Test
    public void getMethodNotSupported() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setMethod("put");
        assertNull(HttpMethod.getMethodIfSupported(servletRequest, EnumSet.noneOf(HttpMethod.class)));
    }

    @Test
    public void getMethodRequestNotExists() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setMethod("test");
        assertNull(HttpMethod.getMethodIfSupported(servletRequest, EnumSet.noneOf(HttpMethod.class)));
    }
}
