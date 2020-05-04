package me.desair.tus.server.upload;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class UUIDUploadIdFactoryTest {

    private UploadIdFactory idFactory;

    @BeforeEach
    public void setUp() {
        idFactory = new UUIDUploadIdFactory();
    }

    @Test
    public void setUploadURINull() {
        Assertions.assertThrows(NullPointerException.class, () -> idFactory.setUploadURI(null));
    }

    @Test
    public void setUploadURINoTrailingSlash() {
        idFactory.setUploadURI("/test/upload");
        assertThat(idFactory.getUploadURI(), is("/test/upload"));
    }

    @Test
    public void setUploadURIWithTrailingSlash() {
        idFactory.setUploadURI("/test/upload/");
        assertThat(idFactory.getUploadURI(), is("/test/upload/"));
    }

    @Test
    public void setUploadURIBlank() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> idFactory.setUploadURI(" "));
    }

    @Test
    public void setUploadURINoStartingSlash() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> idFactory.setUploadURI("test/upload/"));
    }

    @Test
    public void setUploadURIEndsWithDollar() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> idFactory.setUploadURI("/test/upload$"));
    }

    @Test
    public void readUploadId() {
        idFactory.setUploadURI("/test/upload");
        assertThat(idFactory.readUploadId("/test/upload/1911e8a4-6939-490c-b58b-a5d70f8d91fb"), hasToString("1911e8a4-6939-490c-b58b-a5d70f8d91fb"));
    }

    @Test
    public void readUploadIdRegex() {
        idFactory.setUploadURI("/users/[0-9]+/files/upload");
        assertThat(idFactory.readUploadId("/users/1337/files/upload/1911e8a4-6939-490c-b58b-a5d70f8d91fb"), hasToString("1911e8a4-6939-490c-b58b-a5d70f8d91fb"));
    }

    @Test
    public void readUploadIdTrailingSlash() {
        idFactory.setUploadURI("/test/upload/");
        assertThat(idFactory.readUploadId("/test/upload/1911e8a4-6939-490c-b58b-a5d70f8d91fb"), hasToString("1911e8a4-6939-490c-b58b-a5d70f8d91fb"));
    }

    @Test
    public void readUploadIdRegexTrailingSlash() {
        idFactory.setUploadURI("/users/[0-9]+/files/upload/");
        assertThat(idFactory.readUploadId("/users/123456789/files/upload/1911e8a4-6939-490c-b58b-a5d70f8d91fb"), hasToString("1911e8a4-6939-490c-b58b-a5d70f8d91fb"));
    }

    @Test
    public void readUploadIdNoUUID() {
        idFactory.setUploadURI("/test/upload");
        assertThat(idFactory.readUploadId("/test/upload/not-a-uuid-value"), is(nullValue()));
    }

    @Test
    public void readUploadIdRegexNoMatch() {
        idFactory.setUploadURI("/users/[0-9]+/files/upload");
        assertThat(idFactory.readUploadId("/users/files/upload/1911e8a4-6939-490c-b58b-a5d70f8d91fb"), is(nullValue()));
    }

    @Test
    public void createId() {
        assertThat(idFactory.createId(), not(nullValue()));
    }
}
