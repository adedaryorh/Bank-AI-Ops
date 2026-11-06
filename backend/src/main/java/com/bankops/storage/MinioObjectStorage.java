package com.bankops.storage;

import com.bankops.common.WorkspaceException;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Minimal S3 SigV4 transport for our private MinIO bucket. No public object URLs. */
@Component
public class MinioObjectStorage implements ObjectStorage {
    private final URI endpoint;private final String accessKey,secretKey,bucket;
    private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
        .followRedirects(HttpClient.Redirect.NEVER).build();
    private record Response(int statusCode,byte[] body) {}
    private volatile boolean ready;
    public MinioObjectStorage(@Value("${bankops.storage.endpoint:http://localhost:9000}") String endpoint,
        @Value("${bankops.storage.access-key:}") String accessKey,@Value("${bankops.storage.secret-key:}") String secretKey,
        @Value("${bankops.storage.bucket:bankops-documents}") String bucket) {
        this.endpoint=URI.create(endpoint.replaceAll("/+$",""));this.accessKey=accessKey;this.secretKey=secretKey;this.bucket=bucket;
        if(!List.of("http","https").contains(this.endpoint.getScheme()) || this.endpoint.getHost()==null
            || this.endpoint.getUserInfo()!=null || this.endpoint.getQuery()!=null || this.endpoint.getFragment()!=null
            || (this.endpoint.getPath()!=null && !this.endpoint.getPath().isEmpty()) || !bucket.matches("[a-z0-9][a-z0-9-]{2,62}"))
            throw new IllegalArgumentException("Invalid object storage endpoint or bucket.");
    }
    private synchronized void ensureBucket() {
        if(ready)return;
        var result=request("PUT","/"+bucket,new byte[0]);
        if(result.statusCode()!=200 && result.statusCode()!=204 &&
            !(result.statusCode()==409 && request("HEAD","/"+bucket,new byte[0]).statusCode()==200)) throw unavailable();
        ready=true;
    }
    @Override public void put(String key,byte[] data) { ensureBucket();checkKey(key);expect(request("PUT","/"+bucket+"/"+key,data),200); }
    @Override public byte[] get(String key) { checkKey(key);var response=request("GET","/"+bucket+"/"+key,new byte[0]);expect(response,200);return response.body(); }
    @Override public void delete(String key) { checkKey(key);expect(request("DELETE","/"+bucket+"/"+key,new byte[0]),204); }
    private void checkKey(String key) { if(!key.matches("[a-f0-9-]{36}/source.txt")) throw new IllegalArgumentException("Invalid object key."); }
    private void expect(Response response,int status) { if(response.statusCode()!=status)throw unavailable(); }
    private WorkspaceException unavailable() { return new WorkspaceException(HttpStatus.SERVICE_UNAVAILABLE,"Document storage is unavailable. Check MinIO configuration."); }
    private Response request(String method,String path,byte[] body) {
        if(accessKey.isBlank() || secretKey.isBlank())throw unavailable();
        try {
            URI uri=URI.create(endpoint+path);String host=uri.getRawAuthority();
            String stamp=DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC).format(Instant.now());
            String day=stamp.substring(0,8),hash=sha256(body);
            String headers="host:"+host+"\n"+"x-amz-content-sha256:"+hash+"\n"+"x-amz-date:"+stamp+"\n";
            String signed="host;x-amz-content-sha256;x-amz-date";
            String canonical=method+"\n"+path+"\n\n"+headers+"\n"+signed+"\n"+hash;
            String scope=day+"/us-east-1/s3/aws4_request";
            String toSign="AWS4-HMAC-SHA256\n"+stamp+"\n"+scope+"\n"+sha256(canonical.getBytes(StandardCharsets.UTF_8));
            byte[] date=hmac(("AWS4"+secretKey).getBytes(StandardCharsets.UTF_8),day);
            byte[] region=hmac(date,"us-east-1"),service=hmac(region,"s3"),signing=hmac(service,"aws4_request");
            String auth="AWS4-HMAC-SHA256 Credential="+accessKey+"/"+scope+", SignedHeaders="+signed+", Signature="+HexFormat.of().formatHex(hmac(signing,toSign));
            var req=HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(30)).header("x-amz-date",stamp)
                .header("x-amz-content-sha256",hash).header("Authorization",auth)
                .method(method,HttpRequest.BodyPublishers.ofByteArray(body)).build();
            var response=client.send(req,HttpResponse.BodyHandlers.ofInputStream());
            try(var stream=response.body()) {
                byte[] data=stream.readNBytes(1048577);
                if(data.length>1048576)throw unavailable();
                return new Response(response.statusCode(),data);
            }
        } catch(InterruptedException ex) { Thread.currentThread().interrupt();throw unavailable(); }
          catch(Exception ex) { throw unavailable(); }
    }
    public static String sha256(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch(java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
    private static byte[] hmac(byte[] key,String value) throws Exception {
        Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(key,"HmacSHA256"));return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
    }
}
