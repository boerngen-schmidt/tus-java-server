package me.desair.tus.server.upload;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class UploadIdTest {

    @Test
    public void getOriginalObjectUUID() {
        UUID id = UUID.randomUUID();
        UploadId uploadId = new UploadId(id);
        assertEquals(id.toString(), uploadId.toString());
        assertEquals(id, uploadId.getOriginalObject());
    }

    @Test
    public void getOriginalObjectLong() {
        UploadId uploadId = new UploadId(1337L);
        assertEquals("1337", uploadId.toString());
        assertEquals(1337L, uploadId.getOriginalObject());
    }

    @Test
    public void testNullConstructor() {
        Assertions.assertThrows(NullPointerException.class, () -> new UploadId(null));
    }

    @Test
    public void testBlankConstructor() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new UploadId(" \t"));
    }

    @Test
    public void toStringNotYetUrlSafe() {
        UploadId uploadId = new UploadId("my test id/1");
        assertEquals("my+test+id%2F1", uploadId.toString());
    }

    @Test
    public void toStringNotYetUrlSafe2() {
        UploadId uploadId = new UploadId("id+%2F1+/+1");
        assertEquals("id+%2F1+/+1", uploadId.toString());
    }

    @Test
    public void toStringAlreadyUrlSafe() {
        UploadId uploadId = new UploadId("my+test+id%2F1");
        assertEquals("my+test+id%2F1", uploadId.toString());
    }

    @Test
    public void toStringWithInternalDecoderException() {
        String test = "Invalid % value";
        UploadId id = new UploadId(test);
        assertEquals("Invalid % value", id.toString());
    }

    @Test
    public void equalsSameUrlSafeValue() {
        UploadId id1 = new UploadId("id%2F1");
        UploadId id2 = new UploadId("id/1");
        UploadId id3 = new UploadId("id/1");
        assertEquals(id1, id2);
        assertEquals(id2, id3);
        assertEquals(id1, id1);
        assertNotEquals(id1, null);
        assertNotEquals(id1, UUID.randomUUID());
    }

    @Test
    public void hashCodeSameUrlSafeValue() {
        UploadId id1 = new UploadId("id%2F1");
        UploadId id2 = new UploadId("id/1");
        UploadId id3 = new UploadId("id/1");
        assertEquals(id1.hashCode(), id2.hashCode());
        assertEquals(id2.hashCode(), id3.hashCode());
    }
}
