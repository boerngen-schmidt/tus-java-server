package me.desair.tus.server.upload;

import java.util.Optional;
import java.util.regex.Pattern;

public interface UploadIdService {
    String UploadIdMatcher = "tusId";

    UploadId newUploadId();

    /**
     * Extract TUS UploadId from requst URI
     *
     * @param requestUri the request URI string from {@link javax.servlet.http.HttpServletRequest#getRequestURI()}
     * @param baseURI The TUS FileUploads endpoint matching pattern
     * @return If TUS UploadId can be extracted
     */
    Optional<UploadId> fromRequestUri(String requestUri, Pattern baseURI);

    /**
     * Returns a regex pattern for the TUS UploadId in the URI Path
     *
     * The named capture group must be named "tusId".
     * The Patter must start with "/" and must be outside the named capture group
     *
     * Example:
     *      The TUS UploadID is a number like in https://example.com/app/upload/42
     *      A vaild return value: "/(?<tusId>\d+)"
     *
     *
     *      If the TUS UploadId is a UUID4 https://example.com/app/user/42/upload/b5a40d4f-5618-4a7f-8356-b019b6c049c1
     *      A vaild return value: "/(?<tusId>[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12})"
     *
     * Usage:
     *      {@link javax.servlet.http.HttpServletRequest#getRequestURI()} will return "/user/42/upload/b5a40d4f-5618-4a7f-8356-b019b6c049c1"
     *      TUS UploadId extraction pattern will be composed of a base URI pattern ("/user/\d+/upload") and {@link UploadIdService#getTusIdRegex()}
     *
     * @return compilable {@link java.util.regex.Pattern} with named capturing group "tusId"
     */
    String getTusIdRegex();

}
