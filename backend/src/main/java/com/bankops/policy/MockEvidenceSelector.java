package com.bankops.policy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.util.List;
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnExpression("'${bankops.ai.provider:mock}' == 'mock' || '${bankops.ai.provider:mock}' == ''")
public class MockEvidenceSelector implements EvidenceSelector {
    @Override public Selection select(String question,List<Candidate> candidates) {
        return new Selection(candidates.stream().limit(3).map(Candidate::id).toList(),null);
    }
    @Override public String model() { return "deterministic-extractive-mock-v1"; }
}
