package me.desair.tus.server.concatenation.validation;

import me.desair.tus.server.HttpHeader;
import me.desair.tus.server.HttpMethod;
import me.desair.tus.server.exception.InvalidPartialUploadIdException;
import me.desair.tus.server.upload.UploadId;
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

import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class PartialUploadsExistValidatorTest {

    private PartialUploadsExistValidator validator;

    private MockHttpServletRequest servletRequest;

    @Mock
    private UploadStorageService uploadStorageService;

    @BeforeEach
    public void setUp() {
        servletRequest = new MockHttpServletRequest();
        validator = new PartialUploadsExistValidator();
    }

    @Test
    public void supports() throws Exception {
        assertThat(validator.supports(HttpMethod.GET), is(false));
        assertThat(validator.supports(HttpMethod.POST), is(true));
        assertThat(validator.supports(HttpMethod.PUT), is(false));
        assertThat(validator.supports(HttpMethod.DELETE), is(false));
        assertThat(validator.supports(HttpMethod.HEAD), is(false));
        assertThat(validator.supports(HttpMethod.OPTIONS), is(false));
        assertThat(validator.supports(HttpMethod.PATCH), is(false));
        assertThat(validator.supports(null), is(false));
    }

    @Test
    public void testValid() throws Exception {
        UploadInfo info1 = new UploadInfo();
        info1.setId(new UploadId(UUID.randomUUID()));
        UploadInfo info2 = new UploadInfo();
        info2.setId(new UploadId(UUID.randomUUID()));
        when(uploadStorageService.getUploadInfo(info1.getId().toString(), null)).thenReturn(info1);
        when(uploadStorageService.getUploadInfo(info2.getId().toString(), null)).thenReturn(info2);
        servletRequest.addHeader(HttpHeader.UPLOAD_CONCAT, String.format("final; %s %s", info1.getId(), info2.getId()));
        //When we validate the request
        validator.validate(HttpMethod.POST, servletRequest, uploadStorageService, null);
    //No exception is thrown
    }

    @Test
    public void testInvalidUploadNotFound() throws Exception {
        Assertions.assertThrows(InvalidPartialUploadIdException.class, () -> {
            UploadInfo info1 = new UploadInfo();
            info1.setId(new UploadId(UUID.randomUUID()));
            when(uploadStorageService.getUploadInfo(info1.getId())).thenReturn(info1);
            servletRequest.addHeader(HttpHeader.UPLOAD_CONCAT, String.format("final; %s %s", info1.getId(), UUID.randomUUID()));
            //When we validate the request
            validator.validate(HttpMethod.POST, servletRequest, uploadStorageService, null);
        });
    }

    @Test
    public void testInvalidId() throws Exception {
        Assertions.assertThrows(InvalidPartialUploadIdException.class, () -> {
            UploadInfo info1 = new UploadInfo();
            info1.setId(new UploadId(UUID.randomUUID()));
            when(uploadStorageService.getUploadInfo(info1.getId().toString(), null)).thenReturn(info1);
            servletRequest.addHeader(HttpHeader.UPLOAD_CONCAT, String.format("final; %s %s", info1.getId(), "test"));
            //When we validate the request
            validator.validate(HttpMethod.POST, servletRequest, uploadStorageService, null);
        });
    }

    @Test
    public void testInvalidNoUploads1() throws Exception {
        Assertions.assertThrows(InvalidPartialUploadIdException.class, () -> {
            servletRequest.addHeader(HttpHeader.UPLOAD_CONCAT, "final;   ");
            //When we validate the request
            validator.validate(HttpMethod.POST, servletRequest, uploadStorageService, null);
            //No Exception is thrown
        });
    }

    @Test
    public void testInvalidNoUploads2() throws Exception {
        Assertions.assertThrows(InvalidPartialUploadIdException.class, () -> {
            servletRequest.addHeader(HttpHeader.UPLOAD_CONCAT, "final;");
            //When we validate the request
            validator.validate(HttpMethod.POST, servletRequest, uploadStorageService, null);
            //No Exception is thrown
        });
    }
}
