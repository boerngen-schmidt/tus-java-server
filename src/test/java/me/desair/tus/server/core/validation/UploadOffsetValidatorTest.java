package me.desair.tus.server.core.validation;

import me.desair.tus.server.HttpHeader;
import me.desair.tus.server.HttpMethod;
import me.desair.tus.server.exception.UploadOffsetMismatchException;
import me.desair.tus.server.upload.UploadInfo;
import me.desair.tus.server.upload.UploadStorageService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class UploadOffsetValidatorTest {

    private UploadOffsetValidator validator;

    private MockHttpServletRequest servletRequest;

    @Mock
    private UploadStorageService uploadStorageService;

    @BeforeEach
    public void setUp() {
        servletRequest = new MockHttpServletRequest();
        validator = new UploadOffsetValidator();
    }

    @Test
    public void validateValidOffsetInitialUpload() throws Exception {
        UploadInfo info = new UploadInfo();
        info.setOffset(0L);
        info.setLength(10L);
        when(uploadStorageService.getUploadInfo(nullable(String.class), nullable(String.class))).thenReturn(info);
        servletRequest.addHeader(HttpHeader.UPLOAD_OFFSET, 0L);
        //When we validate the request
        try {
            validator.validate(HttpMethod.PATCH, servletRequest, uploadStorageService, null);
        } catch (Exception ex) {
            fail();
        }
    //No Exception is thrown
    }

    @Test
    public void validateValidOffsetInProgressUpload() throws Exception {
        UploadInfo info = new UploadInfo();
        info.setOffset(5L);
        info.setLength(10L);
        when(uploadStorageService.getUploadInfo(nullable(String.class), nullable(String.class))).thenReturn(info);
        servletRequest.addHeader(HttpHeader.UPLOAD_OFFSET, 5L);
        //When we validate the request
        try {
            validator.validate(HttpMethod.PATCH, servletRequest, uploadStorageService, null);
        } catch (Exception ex) {
            fail();
        }
    //No Exception is thrown
    }

    @Test
    public void validateInvalidOffsetInitialUpload() throws Exception {
        Assertions.assertThrows(UploadOffsetMismatchException.class, () -> {
            UploadInfo info = new UploadInfo();
            info.setOffset(0L);
            info.setLength(10L);
            when(uploadStorageService.getUploadInfo(nullable(String.class), nullable(String.class))).thenReturn(info);
            servletRequest.addHeader(HttpHeader.UPLOAD_OFFSET, 3L);
            //When we validate the request
            validator.validate(HttpMethod.PATCH, servletRequest, uploadStorageService, null);
            //Then expect a UploadOffsetMismatchException exception
        });
    }

    @Test
    public void validateInvalidOffsetInProgressUpload() throws Exception {
        Assertions.assertThrows(UploadOffsetMismatchException.class, () -> {
            UploadInfo info = new UploadInfo();
            info.setOffset(5L);
            info.setLength(10L);
            when(uploadStorageService.getUploadInfo(nullable(String.class), nullable(String.class))).thenReturn(info);
            servletRequest.addHeader(HttpHeader.UPLOAD_OFFSET, 6L);
            //When we validate the request
            validator.validate(HttpMethod.PATCH, servletRequest, uploadStorageService, null);
            //Then expect a UploadOffsetMismatchException exception
        });
    }

    @Test
    public void validateMissingUploadOffset() throws Exception {
        Assertions.assertThrows(UploadOffsetMismatchException.class, () -> {
            UploadInfo info = new UploadInfo();
            info.setOffset(2L);
            info.setLength(10L);
            when(uploadStorageService.getUploadInfo(nullable(String.class), nullable(String.class))).thenReturn(info);
            //We don't set a content length header
            //servletRequest.addHeader(HttpHeader.UPLOAD_OFFSET, 3L);
            //When we validate the request
            validator.validate(HttpMethod.PATCH, servletRequest, uploadStorageService, null);
            //Then expect a UploadOffsetMismatchException exception
        });
    }

    @Test
    public void validateMissingUploadInfo() throws Exception {
        when(uploadStorageService.getUploadInfo(nullable(String.class), nullable(String.class))).thenReturn(null);
        servletRequest.addHeader(HttpHeader.UPLOAD_OFFSET, 3L);
        //When we validate the request
        try {
            validator.validate(HttpMethod.PATCH, servletRequest, uploadStorageService, null);
        } catch (Exception ex) {
            fail();
        }
    //No Exception is thrown
    }

    @Test
    public void supports() throws Exception {
        assertThat(validator.supports(HttpMethod.GET), is(false));
        assertThat(validator.supports(HttpMethod.POST), is(false));
        assertThat(validator.supports(HttpMethod.PUT), is(false));
        assertThat(validator.supports(HttpMethod.DELETE), is(false));
        assertThat(validator.supports(HttpMethod.HEAD), is(false));
        assertThat(validator.supports(HttpMethod.OPTIONS), is(false));
        assertThat(validator.supports(HttpMethod.PATCH), is(true));
        assertThat(validator.supports(null), is(false));
    }
}
