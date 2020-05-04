package me.desair.tus.server.core;

import me.desair.tus.server.HttpHeader;
import me.desair.tus.server.HttpMethod;
import me.desair.tus.server.TusFileUploadService;
import me.desair.tus.server.upload.UploadStorageService;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CoreOptionsRequestHandlerTest {

    private CoreOptionsRequestHandler handler;

    private MockHttpServletRequest servletRequest;

    private MockHttpServletResponse servletResponse;

    @Mock
    private UploadStorageService uploadStorageService;

    @BeforeEach
    public void setUp() {
        servletRequest = new MockHttpServletRequest();
        servletResponse = new MockHttpServletResponse();
        handler = new CoreOptionsRequestHandler();
    }

    @Test
    public void processWithMaxSize() throws Exception {
        when(uploadStorageService.getMaxUploadSize()).thenReturn(5368709120L);
        handler.process(HttpMethod.OPTIONS, new TusServletRequest(servletRequest), new TusServletResponse(servletResponse), uploadStorageService, null);
        assertThat(servletResponse.getHeader(HttpHeader.TUS_VERSION), is(TusFileUploadService.TUS_API_VERSION));
        assertThat(servletResponse.getHeader(HttpHeader.TUS_MAX_SIZE), is("5368709120"));
        assertThat(servletResponse.getStatus(), is(HttpServletResponse.SC_NO_CONTENT));
    }

    @Test
    public void processWithoutMaxSize() throws Exception {
        when(uploadStorageService.getMaxUploadSize()).thenReturn(0L);
        handler.process(HttpMethod.OPTIONS, new TusServletRequest(servletRequest), new TusServletResponse(servletResponse), uploadStorageService, null);
        assertThat(servletResponse.getHeader(HttpHeader.TUS_VERSION), is("1.0.0"));
        assertThat(servletResponse.getHeader(HttpHeader.TUS_MAX_SIZE), is(nullValue()));
        assertThat(servletResponse.getStatus(), is(HttpServletResponse.SC_NO_CONTENT));
    }

    @Test
    public void supports() throws Exception {
        assertThat(handler.supports(HttpMethod.GET), is(false));
        assertThat(handler.supports(HttpMethod.POST), is(false));
        assertThat(handler.supports(HttpMethod.PUT), is(false));
        assertThat(handler.supports(HttpMethod.DELETE), is(false));
        assertThat(handler.supports(HttpMethod.HEAD), is(false));
        assertThat(handler.supports(HttpMethod.OPTIONS), is(true));
        assertThat(handler.supports(HttpMethod.PATCH), is(false));
        assertThat(handler.supports(null), is(false));
    }
}
