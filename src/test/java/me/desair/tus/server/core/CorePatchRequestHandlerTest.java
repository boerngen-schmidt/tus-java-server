package me.desair.tus.server.core;

import me.desair.tus.server.HttpHeader;
import me.desair.tus.server.HttpMethod;
import me.desair.tus.server.exception.UploadNotFoundException;
import me.desair.tus.server.upload.UploadId;
import me.desair.tus.server.upload.UploadInfo;
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
import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CorePatchRequestHandlerTest {

    private CorePatchRequestHandler handler;

    private MockHttpServletRequest servletRequest;

    private MockHttpServletResponse servletResponse;

    @Mock
    private UploadStorageService uploadStorageService;

    @BeforeEach
    public void setUp() {
        servletRequest = new MockHttpServletRequest();
        servletResponse = new MockHttpServletResponse();
        handler = new CorePatchRequestHandler();
    }

    @Test
    public void supports() throws Exception {
        assertThat(handler.supports(HttpMethod.GET), is(false));
        assertThat(handler.supports(HttpMethod.POST), is(false));
        assertThat(handler.supports(HttpMethod.PUT), is(false));
        assertThat(handler.supports(HttpMethod.DELETE), is(false));
        assertThat(handler.supports(HttpMethod.HEAD), is(false));
        assertThat(handler.supports(HttpMethod.OPTIONS), is(false));
        assertThat(handler.supports(HttpMethod.PATCH), is(true));
        assertThat(handler.supports(null), is(false));
    }

    @Test
    public void processInProgress() throws Exception {
        UploadInfo info = new UploadInfo();
        info.setId(new UploadId(UUID.randomUUID()));
        info.setOffset(2L);
        info.setLength(10L);
        when(uploadStorageService.getUploadInfo(nullable(String.class), nullable(String.class))).thenReturn(java.util.Optional.of(info));
        UploadInfo updatedInfo = new UploadInfo();
        updatedInfo.setId(info.getId());
        updatedInfo.setOffset(8L);
        updatedInfo.setLength(10L);
        when(uploadStorageService.append(any(UploadInfo.class), any(InputStream.class))).thenReturn(updatedInfo);
        handler.process(HttpMethod.PATCH, new TusServletRequest(servletRequest), new TusServletResponse(servletResponse), uploadStorageService, null);
        verify(uploadStorageService, times(1)).append(eq(info), any(InputStream.class));
        assertThat(servletResponse.getHeader(HttpHeader.UPLOAD_OFFSET), is("8"));
        assertThat(servletResponse.getStatus(), is(HttpServletResponse.SC_NO_CONTENT));
    }

    @Test
    public void processFinished() throws Exception {
        UploadInfo info = new UploadInfo();
        info.setId(new UploadId(UUID.randomUUID()));
        info.setOffset(10L);
        info.setLength(10L);
        when(uploadStorageService.getUploadInfo(nullable(String.class), nullable(String.class))).thenReturn(java.util.Optional.of(info));
        handler.process(HttpMethod.PATCH, new TusServletRequest(servletRequest), new TusServletResponse(servletResponse), uploadStorageService, null);
        verify(uploadStorageService, never()).append(any(UploadInfo.class), any(InputStream.class));
        assertThat(servletResponse.getHeader(HttpHeader.UPLOAD_OFFSET), is("10"));
        assertThat(servletResponse.getStatus(), is(HttpServletResponse.SC_NO_CONTENT));
    }

    @Test
    public void processNotFound() throws Exception {
        when(uploadStorageService.getUploadInfo(nullable(String.class), nullable(String.class))).thenReturn(Optional.empty());
        handler.process(HttpMethod.PATCH, new TusServletRequest(servletRequest), new TusServletResponse(servletResponse), uploadStorageService, null);
        verify(uploadStorageService, never()).append(any(UploadInfo.class), any(InputStream.class));
        assertThat(servletResponse.getStatus(), is(HttpServletResponse.SC_INTERNAL_SERVER_ERROR));
    }

    @Test
    public void processAppendNotFound() throws Exception {
        UploadInfo info = new UploadInfo();
        info.setId(new UploadId(UUID.randomUUID()));
        info.setOffset(10L);
        info.setLength(8L);
        when(uploadStorageService.getUploadInfo(nullable(String.class), nullable(String.class))).thenReturn(java.util.Optional.of(info));
        when(uploadStorageService.append(any(UploadInfo.class), any(InputStream.class))).thenThrow(new UploadNotFoundException("test"));
        handler.process(HttpMethod.PATCH, new TusServletRequest(servletRequest), new TusServletResponse(servletResponse), uploadStorageService, null);
        assertThat(servletResponse.getStatus(), is(HttpServletResponse.SC_INTERNAL_SERVER_ERROR));
    }
}
