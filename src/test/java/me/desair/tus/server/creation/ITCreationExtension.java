package me.desair.tus.server.creation;

import me.desair.tus.server.AbstractTusExtensionIntegrationTest;
import me.desair.tus.server.HttpHeader;
import me.desair.tus.server.HttpMethod;
import me.desair.tus.server.exception.InvalidUploadLengthException;
import me.desair.tus.server.exception.MaxUploadLengthExceededException;
import me.desair.tus.server.exception.PostOnInvalidRequestURIException;
import me.desair.tus.server.upload.UploadId;
import me.desair.tus.server.upload.UploadInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.stubbing.Answer;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.*;

public class ITCreationExtension extends AbstractTusExtensionIntegrationTest {

    private static final String UPLOAD_URI = "/test/upload";

    //It's important to return relative UPLOAD URLs in the Location header in order to support HTTPS proxies
    //that sit in front of the web app
    private static final String UPLOAD_URL = UPLOAD_URI + "/";

    private UploadId id;

    @BeforeEach
    public void setUp() throws Exception {
        servletRequest = new MockHttpServletRequest();
        servletResponse = new MockHttpServletResponse();
        tusFeature = new CreationExtension();
        uploadInfo = null;
        id = new UploadId(UUID.randomUUID());
        servletRequest.setRequestURI(UPLOAD_URI);
        reset(uploadStorageService);
        when(uploadStorageService.getUploadURI()).thenReturn(UPLOAD_URI);
        when(uploadStorageService.create(ArgumentMatchers.any(UploadInfo.class), nullable(String.class))).then((Answer<UploadInfo>) invocation -> {
            UploadInfo upload = invocation.getArgument(0);
            upload.setId(id);
            when(uploadStorageService.getUploadInfo(UPLOAD_URL + id.toString(), invocation.getArgument(1))).thenReturn(java.util.Optional.of(upload));
            return upload;
        });
    }

    @Test
    public void testOptions() throws Exception {
        setRequestHeaders();
        executeCall(HttpMethod.OPTIONS, false);
        //If the Server supports this extension, it MUST add creation to the Tus-Extension header.
        //If the Server supports deferring length, it MUST add creation-defer-length to the Tus-Extension header.
        assertResponseHeader(HttpHeader.TUS_EXTENSION, "creation", "creation-defer-length");
    }

    @Test
    public void testPostWithLength() throws Exception {
        //Create upload
        servletRequest.addHeader(HttpHeader.UPLOAD_LENGTH, 9);
        executeCall(HttpMethod.POST, false);
        verify(uploadStorageService, times(1)).create(any(UploadInfo.class), nullable(String.class));
        assertResponseHeader(HttpHeader.LOCATION, UPLOAD_URL + id.toString());
        assertResponseStatus(HttpServletResponse.SC_CREATED);
        //Check data with head request
        servletRequest.setRequestURI(UPLOAD_URL + id.toString());
        servletResponse = new MockHttpServletResponse();
        executeCall(HttpMethod.HEAD, false);
        assertThat(servletResponse.getHeader(HttpHeader.UPLOAD_METADATA), is(nullValue()));
        assertThat(servletResponse.getHeader(HttpHeader.UPLOAD_DEFER_LENGTH), is(nullValue()));
        //Test Patch request
        servletRequest = new MockHttpServletRequest();
        servletRequest.addHeader(HttpHeader.UPLOAD_LENGTH, 9);
        servletRequest.setRequestURI(UPLOAD_URL + id.toString());
        servletResponse = new MockHttpServletResponse();
        executeCall(HttpMethod.PATCH, false);
    }

    @Test
    public void testPostWithDeferredLength() throws Exception {
        //Create upload
        servletRequest.addHeader(HttpHeader.UPLOAD_DEFER_LENGTH, 1);
        executeCall(HttpMethod.POST, false);
        verify(uploadStorageService, times(1)).create(any(UploadInfo.class), nullable(String.class));
        assertResponseHeader(HttpHeader.LOCATION, UPLOAD_URL + id.toString());
        assertResponseStatus(HttpServletResponse.SC_CREATED);
        //Check data with head request
        servletRequest.setRequestURI(UPLOAD_URL + id.toString());
        servletResponse = new MockHttpServletResponse();
        executeCall(HttpMethod.HEAD, false);
        assertThat(servletResponse.getHeader(HttpHeader.UPLOAD_METADATA), is(nullValue()));
        assertThat(servletResponse.getHeader(HttpHeader.UPLOAD_DEFER_LENGTH), is("1"));
        //Test Patch request
        servletRequest = new MockHttpServletRequest();
        servletRequest.addHeader(HttpHeader.UPLOAD_LENGTH, 9);
        servletRequest.setRequestURI(UPLOAD_URL + id.toString());
        servletResponse = new MockHttpServletResponse();
        executeCall(HttpMethod.PATCH, false);
        //Re-check head request
        servletRequest = new MockHttpServletRequest();
        servletRequest.setRequestURI(UPLOAD_URL + id.toString());
        servletResponse = new MockHttpServletResponse();
        executeCall(HttpMethod.HEAD, false);
        assertThat(servletResponse.getHeader(HttpHeader.UPLOAD_METADATA), is(nullValue()));
        assertThat(servletResponse.getHeader(HttpHeader.UPLOAD_DEFER_LENGTH), is(nullValue()));
    }

    @Test
    public void testPostWithoutLength() {
        Assertions.assertThrows(InvalidUploadLengthException.class, () -> {
            //Create upload without any length header
            executeCall(HttpMethod.POST, false);
        });
    }

    @Test
    public void testPostWithMetadata() throws Exception {
        //Create upload
        servletRequest.addHeader(HttpHeader.UPLOAD_LENGTH, 9);
        servletRequest.addHeader(HttpHeader.UPLOAD_METADATA, "encoded metadata");
        executeCall(HttpMethod.POST, false);
        verify(uploadStorageService, times(1)).create(any(UploadInfo.class), nullable(String.class));
        assertResponseHeader(HttpHeader.LOCATION, UPLOAD_URL + id.toString());
        assertResponseStatus(HttpServletResponse.SC_CREATED);
        //Check data with head request
        servletRequest.setRequestURI(UPLOAD_URL + id.toString());
        servletResponse = new MockHttpServletResponse();
        executeCall(HttpMethod.HEAD, false);
        assertThat(servletResponse.getHeader(HttpHeader.UPLOAD_METADATA), is("encoded metadata"));
        assertThat(servletResponse.getHeader(HttpHeader.UPLOAD_DEFER_LENGTH), is(nullValue()));
    }

    @Test
    public void testPostWithAllowedMaxSize() throws Exception {
        when(uploadStorageService.getMaxUploadSize()).thenReturn(100L);
        //Create upload
        servletRequest.addHeader(HttpHeader.UPLOAD_LENGTH, 90);
        executeCall(HttpMethod.POST, false);
        verify(uploadStorageService, times(1)).create(any(UploadInfo.class), nullable(String.class));
        assertResponseHeader(HttpHeader.LOCATION, UPLOAD_URL + id.toString());
        assertResponseStatus(HttpServletResponse.SC_CREATED);
        //Check data with head request
        servletRequest.setRequestURI(UPLOAD_URL + id.toString());
        servletResponse = new MockHttpServletResponse();
        executeCall(HttpMethod.HEAD, false);
        assertThat(servletResponse.getHeader(HttpHeader.UPLOAD_METADATA), is(nullValue()));
        assertThat(servletResponse.getHeader(HttpHeader.UPLOAD_DEFER_LENGTH), is(nullValue()));
    }

    @Test
    public void testPostWithExceededMaxSize() {
        Assertions.assertThrows(MaxUploadLengthExceededException.class, () -> {
            when(uploadStorageService.getMaxUploadSize()).thenReturn(100L);
            //Create upload
            servletRequest.addHeader(HttpHeader.UPLOAD_LENGTH, 110);
            executeCall(HttpMethod.POST, false);
        });
    }

    @Test
    public void testPostOnInvalidUrl() {
        Assertions.assertThrows(PostOnInvalidRequestURIException.class, () -> {
            //Create upload
            servletRequest.addHeader(HttpHeader.UPLOAD_LENGTH, 9);
            servletRequest.setRequestURI(UPLOAD_URL + id.toString());
            executeCall(HttpMethod.POST, false);
        });
    }

    @Test
    public void testPostWithValidRegexURI() throws Exception {
        reset(uploadStorageService);
        when(uploadStorageService.getUploadURI()).thenReturn("/submission/([a-z0-9]+)/files/upload");
        when(uploadStorageService.create(ArgumentMatchers.any(UploadInfo.class), nullable(String.class))).then((Answer<UploadInfo>) invocation -> {
            UploadInfo upload = invocation.getArgument(0);
            upload.setId(id);
            when(uploadStorageService.getUploadInfo("/submission/0ae5f8vv4s8c/files/upload/" + id.toString(), invocation.getArgument(1))).thenReturn(java.util.Optional.of(upload));
            return upload;
        });
        //Create upload
        servletRequest.setRequestURI("/submission/0ae5f8vv4s8c/files/upload");
        servletRequest.addHeader(HttpHeader.UPLOAD_LENGTH, 9);
        servletRequest.addHeader(HttpHeader.UPLOAD_METADATA, "submission metadata");
        executeCall(HttpMethod.POST, false);
        verify(uploadStorageService, times(1)).create(any(UploadInfo.class), nullable(String.class));
        assertResponseHeader(HttpHeader.LOCATION, "/submission/0ae5f8vv4s8c/files/upload/" + id.toString());
        assertResponseStatus(HttpServletResponse.SC_CREATED);
        //Check data with head request
        servletRequest.setRequestURI("/submission/0ae5f8vv4s8c/files/upload/" + id.toString());
        servletResponse = new MockHttpServletResponse();
        executeCall(HttpMethod.HEAD, false);
        assertThat(servletResponse.getHeader(HttpHeader.UPLOAD_METADATA), is("submission metadata"));
        assertThat(servletResponse.getHeader(HttpHeader.UPLOAD_DEFER_LENGTH), is(nullValue()));
    }

    @Test
    public void testPostWithInvalidRegexURI() {
        Assertions.assertThrows(PostOnInvalidRequestURIException.class, () -> {
            reset(uploadStorageService);
            when(uploadStorageService.getUploadURI()).thenReturn("/submission/([a-z0-9]+)/files/upload");
            when(uploadStorageService.create(ArgumentMatchers.any(UploadInfo.class), nullable(String.class))).then((Answer<UploadInfo>) invocation -> {
                UploadInfo upload = invocation.getArgument(0);
                upload.setId(id);
                when(uploadStorageService.getUploadInfo("/submission/0ae5f8vv4s8c/files/upload/" + id.toString(), invocation.getArgument(1))).thenReturn(java.util.Optional.of(upload));
                return upload;
            });
            //Create upload
            servletRequest.setRequestURI("/submission/a+b/files/upload");
            servletRequest.addHeader(HttpHeader.UPLOAD_LENGTH, 9);
            servletRequest.addHeader(HttpHeader.UPLOAD_METADATA, "submission metadata");
            executeCall(HttpMethod.POST, false);
        });
    }
}
