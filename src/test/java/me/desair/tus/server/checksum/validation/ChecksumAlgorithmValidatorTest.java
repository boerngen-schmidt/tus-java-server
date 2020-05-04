package me.desair.tus.server.checksum.validation;

import me.desair.tus.server.HttpHeader;
import me.desair.tus.server.HttpMethod;
import me.desair.tus.server.exception.ChecksumAlgorithmNotSupportedException;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ChecksumAlgorithmValidatorTest {

    private ChecksumAlgorithmValidator validator;

    private MockHttpServletRequest servletRequest;

    @Mock
    private UploadStorageService uploadStorageService;

    @BeforeEach
    public void setUp() {
        servletRequest = spy(new MockHttpServletRequest());
        validator = new ChecksumAlgorithmValidator();
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

    @Test
    public void testValid() throws Exception {
        servletRequest.addHeader(HttpHeader.UPLOAD_CHECKSUM, "sha1 1234567890");
        validator.validate(HttpMethod.PATCH, servletRequest, uploadStorageService, null);
        verify(servletRequest, times(1)).getHeader(HttpHeader.UPLOAD_CHECKSUM);
    }

    @Test
    public void testNoHeader() throws Exception {
        // TODO change to junit Assertions
        try {
            validator.validate(HttpMethod.PATCH, servletRequest, uploadStorageService, null);
        } catch (Exception ex) {
            fail();
        }
    }

    @Test
    public void testInvalidHeader() throws Exception {
        Assertions.assertThrows(ChecksumAlgorithmNotSupportedException.class, () -> {
            servletRequest.addHeader(HttpHeader.UPLOAD_CHECKSUM, "test 1234567890");
            validator.validate(HttpMethod.PATCH, servletRequest, uploadStorageService, null);
        });
    }
}
