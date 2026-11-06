package com.bankops.policy;
import java.util.List;
import java.util.UUID;
public interface EvidenceSelector {
    record Candidate(UUID id,UUID documentId,String filename,String content) {}
    record Selection(List<UUID> chunkIds,Long tokens) {}
    Selection select(String question,List<Candidate> candidates);
    String model();
}
