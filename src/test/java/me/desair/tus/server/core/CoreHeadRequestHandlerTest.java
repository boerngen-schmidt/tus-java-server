package me.desair.tus.server.core;

import me.desair.tus.server.HttpHeader;
import me.desair.tus.server.HttpMethod;
import me.desair.tus.server.upload.UploadInfo;
import me.desair.tus.server.upload.UploadStorageService;
import me.desair.tus.server.upload.UploadType;
import me.desair.tus.server.util.TusServletRequest;
import me.desair.tus.server.util.TusServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.http.HttpServletResponse;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CoreHeadRequestHandlerTest {

    private CoreHeadRequestHandler handler;

    private MockHttpServletRequest servletRequest;

    private MockHttpServletResponse servletResponse;

    @Mock
    private UploadStorageService uploadStorageService;

    @BeforeEach
    public void setUp() {
        servletRequest = new MockHttpServletRequest();
        servletResponse = new MockHttpServletResponse();
        handler = new CoreHeadRequestHandler();
    }

    @Test
    public void processWithLength() throws Exception {
        UploadInfo info = new UploadInfo();
        info.setOffset(2L);
        info.setLength(10L);
        when(uploadStorageService.getUploadInfo(nullable(String.class), nullable(String.class))).thenReturn(java.util.Optional.of(info));
        handler.process(HttpMethod.HEAD, new TusServletRequest(servletRequest), new TusServletResponse(servletResponse), uploadStorageService, null);
        assertThat(servletResponse.getHeader(HttpHeader.UPLOAD_LENGTH), is("10"));
        assertThat(servletResponse.getHeader(HttpHeader.UPLOAD_OFFSET), is("2"));
        assertThat(servletResponse.getHeader(HttpHeader.CACHE_CONTROL), is("no-store"));
        assertThat(servletResponse.getStatus(), is(HttpServletResponse.SC_NO_CONTENT));
    }

    @Test
    public void processConcatenatedWithLength() throws Exception {
        UploadInfo info = new UploadInfo();
        info.setOffset(2L);
        info.setLength(10L);
        info.setUploadType(UploadType.CONCATENATED);
        when(uploadStorageService.getUploadInfo(nullable(String.class), nullable(String.class))).thenReturn(java.util.Optional.of(info));
        handler.process(HttpMethod.HEAD, new TusServletRequest(servletRequest), new TusServletResponse(servletResponse), uploadStorageService, null);
        assertThat(servletResponse.getHeader(HttpHeader.UPLOAD_LENGTH), is(nullValue()));
        assertThat(servletResponse.getHeader(HttpHeader.UPLOAD_OFFSET), is(nullValue()));
        assertThat(servletResponse.getHeader(HttpHeader.CACHE_CONTROL), is("no-store"));
        assertThat(servletResponse.getStatus(), is(HttpServletResponse.SC_NO_CONTENT));
    }

    @Test
    public void processWithoutLength() throws Exception {
        UploadInfo info = new UploadInfo();
        info.setOffset(0L);
        when(uploadStorageService.getUploadInfo(nullable(String.class), nullable(String.class))).thenReturn(java.util.Optional.of(info));
        handler.process(HttpMethod.HEAD, new TusServletRequest(servletRequest), new TusServletResponse(servletResponse), uploadStorageService, null);
        assertThat(servletResponse.getHeader(HttpHeader.UPLOAD_LENGTH), is(nullValue()));
        assertThat(servletResponse.getHeader(HttpHeader.UPLOAD_OFFSET), is("0"));
        assertThat(servletResponse.getHeader(HttpHeader.CACHE_CONTROL), is("no-store"));
        assertThat(servletResponse.getStatus(), is(HttpServletResponse.SC_NO_CONTENT));
    }

    @Test
    public void supports() throws Exception {
        assertThat(handler.supports(HttpMethod.GET), is(false));
        assertThat(handler.supports(HttpMethod.POST), is(false));
        assertThat(handler.supports(HttpMethod.PUT), is(false));
        assertThat(handler.supports(HttpMethod.DELETE), is(false));
        assertThat(handler.supports(HttpMethod.HEAD), is(true));
        assertThat(handler.supports(HttpMethod.OPTIONS), is(false));
        assertThat(handler.supports(HttpMethod.PATCH), is(false));
        assertThat(handler.supports(null), is(false));
    }
}
