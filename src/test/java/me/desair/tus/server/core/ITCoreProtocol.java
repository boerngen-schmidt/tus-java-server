package me.desair.tus.server.core;

import me.desair.tus.server.AbstractTusExtensionIntegrationTest;
import me.desair.tus.server.HttpHeader;
import me.desair.tus.server.HttpMethod;
import me.desair.tus.server.exception.*;
import me.desair.tus.server.upload.UploadInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ITCoreProtocol extends AbstractTusExtensionIntegrationTest {

    @BeforeEach
    public void setUp() {
        servletRequest = new MockHttpServletRequest();
        servletResponse = new MockHttpServletResponse();
        tusFeature = new CoreProtocol();
        uploadInfo = null;
    }

    @Test
    public void getName() throws Exception {
        assertThat(tusFeature.getName(), is("core"));
    }

    @Test
    public void testUnsupportedHttpMethod() throws Exception {
        Assertions.assertThrows(UnsupportedMethodException.class, () -> {
            prepareUploadInfo(2L, 10L);
            setRequestHeaders(HttpHeader.TUS_RESUMABLE);
            executeCall(HttpMethod.forName("TEST"), false);
        });
    }

    @Test
    public void testHeadWithLength() throws Exception {
        prepareUploadInfo(2L, 10L);
        setRequestHeaders(HttpHeader.TUS_RESUMABLE);
        executeCall(HttpMethod.HEAD, false);
        assertResponseHeader(HttpHeader.TUS_RESUMABLE, "1.0.0");
        assertResponseHeader(HttpHeader.UPLOAD_OFFSET, "2");
        assertResponseHeader(HttpHeader.UPLOAD_LENGTH, "10");
        assertResponseHeader(HttpHeader.CACHE_CONTROL, "no-store");
        assertResponseStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    @Test
    public void testHeadWithoutLength() throws Exception {
        prepareUploadInfo(2L, null);
        setRequestHeaders(HttpHeader.TUS_RESUMABLE);
        executeCall(HttpMethod.HEAD, false);
        assertResponseHeader(HttpHeader.TUS_RESUMABLE, "1.0.0");
        assertResponseHeader(HttpHeader.UPLOAD_OFFSET, "2");
        assertResponseHeader(HttpHeader.UPLOAD_LENGTH, (String) null);
        assertResponseHeader(HttpHeader.CACHE_CONTROL, "no-store");
        assertResponseStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    @Test
    public void testHeadNotFound() throws Exception {
        Assertions.assertThrows(UploadNotFoundException.class, () -> {
            //We don't prepare an upload info
            setRequestHeaders(HttpHeader.TUS_RESUMABLE);
            executeCall(HttpMethod.HEAD, false);
        });
    }

    @Test
    public void testHeadInvalidVersion() throws Exception {
        Assertions.assertThrows(InvalidTusResumableException.class, () -> {
            setRequestHeaders();
            prepareUploadInfo(2L, null);
            servletRequest.addHeader(HttpHeader.TUS_RESUMABLE, "2.0.0");
            executeCall(HttpMethod.HEAD, false);
        });
    }

    @Test
    public void testPatchSuccess() throws Exception {
        prepareUploadInfo(2L, 10L);
        setRequestHeaders(HttpHeader.TUS_RESUMABLE, HttpHeader.CONTENT_TYPE, HttpHeader.UPLOAD_OFFSET, HttpHeader.CONTENT_LENGTH);
        executeCall(HttpMethod.PATCH, false);
        verify(uploadStorageService, times(1)).append(any(UploadInfo.class), any(InputStream.class));
        assertResponseHeader(HttpHeader.TUS_RESUMABLE, "1.0.0");
        assertResponseHeader(HttpHeader.UPLOAD_OFFSET, "2");
        assertResponseHeader(HttpHeader.UPLOAD_LENGTH, (String) null);
        assertResponseHeader(HttpHeader.CACHE_CONTROL, (String) null);
        assertResponseStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    @Test
    public void testPatchInvalidContentType() throws Exception {
        Assertions.assertThrows(InvalidContentTypeException.class, () -> {
            prepareUploadInfo(2L, 10L);
            setRequestHeaders(HttpHeader.TUS_RESUMABLE, HttpHeader.UPLOAD_OFFSET, HttpHeader.CONTENT_LENGTH);
            executeCall(HttpMethod.PATCH, false);
        });
    }

    @Test
    public void testPatchInvalidUploadOffset() throws Exception {
        Assertions.assertThrows(UploadOffsetMismatchException.class, () -> {
            prepareUploadInfo(2L, 10L);
            setRequestHeaders(HttpHeader.TUS_RESUMABLE, HttpHeader.CONTENT_TYPE, HttpHeader.CONTENT_LENGTH);
            servletRequest.addHeader(HttpHeader.UPLOAD_OFFSET, 5);
            executeCall(HttpMethod.PATCH, false);
        });
    }

    @Test
    public void testPatchInvalidContentLength() throws Exception {
        Assertions.assertThrows(InvalidContentLengthException.class, () -> {
            prepareUploadInfo(2L, 10L);
            setRequestHeaders(HttpHeader.TUS_RESUMABLE, HttpHeader.CONTENT_TYPE, HttpHeader.UPLOAD_OFFSET);
            servletRequest.addHeader(HttpHeader.CONTENT_LENGTH, 9);
            executeCall(HttpMethod.PATCH, false);
        });
    }

    @Test
    public void testOptionsWithMaxSize() throws Exception {
        when(uploadStorageService.getMaxUploadSize()).thenReturn(107374182400L);
        setRequestHeaders();
        executeCall(HttpMethod.OPTIONS, false);
        assertResponseHeader(HttpHeader.TUS_RESUMABLE, "1.0.0");
        assertResponseHeader(HttpHeader.TUS_VERSION, "1.0.0");
        assertResponseHeader(HttpHeader.TUS_MAX_SIZE, "107374182400");
        assertResponseHeader(HttpHeader.TUS_EXTENSION, (String) null);
        assertResponseStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    @Test
    public void testOptionsWithNoMaxSize() throws Exception {
        when(uploadStorageService.getMaxUploadSize()).thenReturn(0L);
        setRequestHeaders();
        executeCall(HttpMethod.OPTIONS, false);
        assertResponseHeader(HttpHeader.TUS_RESUMABLE, "1.0.0");
        assertResponseHeader(HttpHeader.TUS_VERSION, "1.0.0");
        assertResponseHeader(HttpHeader.TUS_MAX_SIZE, (String) null);
        assertResponseHeader(HttpHeader.TUS_EXTENSION, (String) null);
        assertResponseStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    @Test
    public void testOptionsIgnoreTusResumable() throws Exception {
        when(uploadStorageService.getMaxUploadSize()).thenReturn(10L);
        setRequestHeaders();
        servletRequest.addHeader(HttpHeader.TUS_RESUMABLE, "2.0.0");
        executeCall(HttpMethod.OPTIONS, false);
        assertResponseHeader(HttpHeader.TUS_RESUMABLE, "1.0.0");
        assertResponseHeader(HttpHeader.TUS_VERSION, "1.0.0");
        assertResponseHeader(HttpHeader.TUS_MAX_SIZE, "10");
        assertResponseHeader(HttpHeader.TUS_EXTENSION, (String) null);
        assertResponseStatus(HttpServletResponse.SC_NO_CONTENT);
    }
}
