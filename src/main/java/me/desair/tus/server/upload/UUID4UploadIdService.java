package me.desair.tus.server.upload;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UUID4UploadIdService implements UploadIdService {

    private static final Pattern UPLOADID_PATTERN;

    private Supplier<UUID> uuidSupplier = UUID::randomUUID;

    static {
        UPLOADID_PATTERN = Pattern.compile("/(?<tusId>[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12})");
    }

    @Override
    public UploadId newUploadId() {
        UUID id = uuidSupplier.get();
        return new UploadId(id);
    }

    @Override
    public Optional<UploadId> fromRequestUri(String requestUri, Pattern baseURI) {
        String fullRequestPattern = baseURI.pattern().concat(UPLOADID_PATTERN.pattern());
        Pattern requestPattern = Pattern.compile(fullRequestPattern);
        Matcher requestMatcher = requestPattern.matcher(requestUri);

        // we are not matching the request URI so we can not extract an UploadId
        if(!requestMatcher.matches()) {
            return Optional.empty();
        }

        String tusUploadId = requestMatcher.group(UploadIdService.UploadIdMatcher);
        return Optional.of(new UploadId(tusUploadId));
    }

    @Override
    public String getTusIdRegex() {
        return UPLOADID_PATTERN.pattern();
    }
}
