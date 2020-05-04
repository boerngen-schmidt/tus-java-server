package me.desair.tus.server.core.validation;

import me.desair.tus.server.HttpHeader;
import me.desair.tus.server.HttpMethod;
import me.desair.tus.server.exception.InvalidContentTypeException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.http.HttpServletRequest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@ExtendWith(MockitoExtension.class)
public class ContentTypeValidatorTest {

    private ContentTypeValidator validator;

    @Spy
    private HttpServletRequest request;

    @BeforeEach
    public void setUp() {
        validator = new ContentTypeValidator();
    }

    @Test
    public void validateValid() throws Exception {
        Mockito.doReturn(ContentTypeValidator.APPLICATION_OFFSET_OCTET_STREAM).when(request).getHeader(HttpHeader.CONTENT_TYPE);
        Assertions.assertDoesNotThrow(() -> validator.validate(HttpMethod.PATCH, request, null, null));
    }

    @Test
    public void validateInvalidHeader() throws Exception {
        Assertions.assertThrows(InvalidContentTypeException.class, () -> {
            Mockito.doReturn("application/octet-stream").when(request).getHeader(HttpHeader.CONTENT_TYPE);
            validator.validate(HttpMethod.PATCH, request, null, null);
            //Expect a InvalidContentTypeException exception
        });
    }

    @Test
    public void validateMissingHeader() throws Exception {
        Assertions.assertThrows(InvalidContentTypeException.class, () -> {
            //We don't set the header
            validator.validate(HttpMethod.PATCH, request, null, null);
            //Expect a InvalidContentTypeException exception
        });
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
