package com.bankops.documents;
import com.bankops.ai.ContentSafety;
import com.bankops.storage.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
@Component
@ConditionalOnProperty(name="bankops.jobs.enabled",havingValue="true",matchIfMissing=true)
public class DocumentWorker {
    private final DocumentJobs jobs;private final ObjectStorage storage;
    public DocumentWorker(DocumentJobs jobs,ObjectStorage storage) { this.jobs=jobs;this.storage=storage; }
    @Scheduled(fixedDelayString="${bankops.jobs.poll-ms:2000}")
    public void poll() {
        var claimed=jobs.claim();if(claimed.isEmpty())return;var job=claimed.get();
        long start=System.nanoTime();
        try {
            byte[] bytes=storage.get(job.key());
            if(bytes.length>1048576 || !MinioObjectStorage.sha256(bytes).equals(job.sha256())) throw new IllegalStateException("Checksum mismatch");
            jobs.complete(job,ContentSafety.text(bytes),(System.nanoTime()-start)/1000000);
        } catch(RuntimeException ex) { jobs.failed(job); }
    }
}
