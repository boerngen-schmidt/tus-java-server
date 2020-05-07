# Goals for Version 3.0

 * Upgrade to minimum required Java to 8
 * Refine Storage Interface with Generics
   * LocalStorage using Path
 * More Examples for current 
 * Support Post and send data?! 

# Documentation

## URI Structure

The tus-java-server's endpoint 
http://localhost:8080/deployment/upload

For each upload tus-java-server will extend the base endpoint with an UploadId
http://localhost:8080/deployment/upload/42