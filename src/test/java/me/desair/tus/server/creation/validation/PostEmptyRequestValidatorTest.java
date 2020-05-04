package me.desair.tus.server.creation.validation;

import me.desair.tus.server.HttpHeader;
import me.desair.tus.server.HttpMethod;
import me.desair.tus.server.exception.InvalidContentLengthException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

public class PostEmptyRequestValidatorTest {

    private PostEmptyRequestValidator validator;

    private MockHttpServletRequest servletRequest;

    @BeforeEach
    public void setUp() {
        servletRequest = new MockHttpServletRequest();
        validator = new PostEmptyRequestValidator();
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
    public void validateMissingContentLength() throws Exception {
        //When we validate the request
        try {
            validator.validate(HttpMethod.POST, servletRequest, null, null);
        } catch (Exception ex) {
            fail();
        }
    //No Exception is thrown
    }

    @Test
    public void validateContentLengthZero() throws Exception {
        servletRequest.addHeader(HttpHeader.CONTENT_LENGTH, 0L);
        //When we validate the request
        try {
            validator.validate(HttpMethod.POST, servletRequest, null, null);
        } catch (Exception ex) {
            fail();
        }
    //No Exception is thrown
    }

    @Test
    public void validateContentLengthNotZero() throws Exception {
        Assertions.assertThrows(InvalidContentLengthException.class, () -> {
            servletRequest.addHeader(HttpHeader.CONTENT_LENGTH, 10L);
            //When we validate the request
            validator.validate(HttpMethod.POST, servletRequest, null, null);
            //Expect a InvalidContentLengthException
        });
    }
}
