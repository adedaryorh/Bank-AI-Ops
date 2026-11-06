package com.bankops;
import com.bankops.storage.MinioObjectStorage;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
class MinioStorageIntegrationTest {
    @Test void privateObjectRoundTripUsesSignedRequests() {
        assertThat(org.testcontainers.DockerClientFactory.instance().isDockerAvailable()).as("Docker is required for MinIO verification").isTrue();
        try(var minio=new GenericContainer<>("cgr.dev/chainguard/minio:latest").withExposedPorts(9000)
            .withEnv("MINIO_ROOT_USER","synthetic-access").withEnv("MINIO_ROOT_PASSWORD","synthetic-secret-123")
            .withCommand("server","/data").waitingFor(Wait.forHttp("/minio/health/live").forPort(9000))) {
            minio.start();var storage=new MinioObjectStorage("http://"+minio.getHost()+":"+minio.getMappedPort(9000),"synthetic-access","synthetic-secret-123","synthetic-documents");
            String key=UUID.randomUUID()+"/source.txt";byte[] data="Synthetic private policy".getBytes(StandardCharsets.UTF_8);
            storage.put(key,data);assertThat(storage.get(key)).isEqualTo(data);storage.delete(key);
            assertThatThrownBy(()->storage.get(key)).isInstanceOf(RuntimeException.class);
        }
    }
}
