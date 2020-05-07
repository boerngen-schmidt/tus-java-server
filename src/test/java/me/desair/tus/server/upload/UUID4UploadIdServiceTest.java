package me.desair.tus.server.upload;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;

@Execution(ExecutionMode.CONCURRENT)
class UUID4UploadIdServiceTest {

    private UUID4UploadIdService service;
    private UUID testUUID;

    @BeforeEach
    public void setUp() {
        service = new UUID4UploadIdService();
        testUUID = UUID.randomUUID();
    }

    @Test
    public void newUploadId() throws NoSuchFieldException, IllegalAccessException {
        @SuppressWarnings("unchecked")
        Supplier<UUID> uuidSupplier = Mockito.mock(Supplier.class);
        Field uuidSupplierField = UUID4UploadIdService.class.getDeclaredField("uuidSupplier");
        uuidSupplierField.setAccessible(true);
        uuidSupplierField.set(service, uuidSupplier);

        Mockito.when(uuidSupplier.get()).thenReturn(testUUID);
        UploadId uploadId = service.newUploadId();
        Assertions.assertEquals(uploadId.toString(), testUUID.toString());
    }

    @Test
    public void fromRequestUriWithValid() {
        String base = "/app/upload";
        Pattern baseURI = Pattern.compile(base);

        // Positive Test
        String requestUri = base + "/" + testUUID.toString();
        Optional<UploadId> uploadId = service.fromRequestUri(requestUri, baseURI);
        Assertions.assertTrue(uploadId.isPresent());
        Assertions.assertEquals(uploadId.get().toString(), testUUID.toString());
    }

    @Test
    public void fromRequestUriWithInvalid() {
        String base = "/app/upload";
        Pattern baseURI = Pattern.compile(base);

        // invalid UUID
        String wrongUUID = base + "/1234-I-am-not-Vaild4321";
        Optional<UploadId> failId = service.fromRequestUri(wrongUUID, baseURI);
        assertFalse(failId.isPresent());

        // Invalid Base request URI
        String wrongBase = "/noapp/invalid/" + testUUID.toString();
        Optional<UploadId> failBase = service.fromRequestUri(wrongBase, baseURI);
        assertFalse(failBase.isPresent());
    }
}