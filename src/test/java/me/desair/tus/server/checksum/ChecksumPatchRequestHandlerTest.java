package me.desair.tus.server.checksum;

import me.desair.tus.server.HttpHeader;
import me.desair.tus.server.HttpMethod;
import me.desair.tus.server.exception.ChecksumAlgorithmNotSupportedException;
import me.desair.tus.server.exception.UploadChecksumMismatchException;
import me.desair.tus.server.upload.UploadInfo;
import me.desair.tus.server.upload.UploadStorageService;
import me.desair.tus.server.util.TusServletRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ChecksumPatchRequestHandlerTest {

    private ChecksumPatchRequestHandler handler;

    @Mock
    private TusServletRequest servletRequest;

    @Mock
    private UploadStorageService uploadStorageService;

    @BeforeEach
    public void setUp() throws Exception {
        handler = new ChecksumPatchRequestHandler();
        UploadInfo info = new UploadInfo();
        info.setOffset(2L);
        info.setLength(10L);
        when(uploadStorageService.getUploadInfo(nullable(String.class), nullable(String.class))).thenReturn(of(info));
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
    public void testValidHeaderAndChecksum() throws Exception {
        when(servletRequest.getHeader(HttpHeader.UPLOAD_CHECKSUM)).thenReturn("sha1 1234567890");
        when(servletRequest.getCalculatedChecksum(ArgumentMatchers.any(ChecksumAlgorithm.class))).thenReturn("1234567890");
        when(servletRequest.hasCalculatedChecksum()).thenReturn(true);
        handler.process(HttpMethod.PATCH, servletRequest, null, uploadStorageService, null);
        verify(servletRequest, times(1)).getCalculatedChecksum(any(ChecksumAlgorithm.class));
    }

    @Test
    public void testValidHeaderAndInvalidChecksum() {
        Assertions.assertThrows(UploadChecksumMismatchException.class, () -> {
            when(servletRequest.getHeader(HttpHeader.UPLOAD_CHECKSUM)).thenReturn("sha1 1234567890");
            when(servletRequest.getCalculatedChecksum(ArgumentMatchers.any(ChecksumAlgorithm.class))).thenReturn("0123456789");
            when(servletRequest.hasCalculatedChecksum()).thenReturn(true);
            handler.process(HttpMethod.PATCH, servletRequest, null, uploadStorageService, null);
        });
    }

    @Test
    public void testNoHeader() throws Exception {
        when(servletRequest.getHeader(HttpHeader.UPLOAD_CHECKSUM)).thenReturn(null);
        handler.process(HttpMethod.PATCH, servletRequest, null, uploadStorageService, null);
        verify(servletRequest, never()).getCalculatedChecksum(any(ChecksumAlgorithm.class));
    }

    @Test
    public void testInvalidHeader() {
        Assertions.assertThrows(ChecksumAlgorithmNotSupportedException.class, () -> {
            when(servletRequest.getHeader(HttpHeader.UPLOAD_CHECKSUM)).thenReturn("test 1234567890");
            when(servletRequest.hasCalculatedChecksum()).thenReturn(true);
            handler.process(HttpMethod.PATCH, servletRequest, null, uploadStorageService, null);
        });
    }
}
