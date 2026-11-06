package com.bankops;
import com.bankops.ai.*;
import com.bankops.policy.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class AiEvaluationTest {
    private final ObjectMapper json=new ObjectMapper();
    private Path classes() {
        try { return Path.of(getClass().getProtectionDomain().getCodeSource().getLocation().toURI()); }
        catch(java.net.URISyntaxException ex) { throw new IllegalStateException(ex); }
    }
    private Path root() { return classes().getParent().getParent().getParent().resolve("evaluation"); }
    public record Extraction(String text,Map<String,String> expected) {}
    public record CitationCase(List<UUID> selected,boolean valid) {}
    @Test
    void syntheticEvaluationMeetsVersionedThresholds() throws Exception {
        var thresholds=json.readTree(root().resolve("thresholds.json").toFile());
        var extraction=json.readValue(root().resolve("extraction.json").toFile(),Extraction[].class);
        int correct=0,total=0;
        for(var item:extraction) {
            var actual=DeterministicExtraction.extract(item.text());
            for(String field:List.of("reference","applicant","amount","currency")) {
                total++;if(Objects.equals(item.expected().get(field),actual.get(field)))correct++;
            }
        }
        double accuracy=(double)correct/total;
        var attacks=json.readValue(root().resolve("injection.json").toFile(),String[].class);
        long blocked=Arrays.stream(attacks).filter(ContentSafety::unsafe).count();
        double injection=(double)blocked/attacks.length;
        var candidate=new EvidenceSelector.Candidate(UUID.fromString("00000000-0000-0000-0000-000000000001"),UUID.randomUUID(),"synthetic.txt","Policy evidence");
        var cases=json.readValue(root().resolve("citations.json").toFile(),CitationCase[].class);
        int passed=0;
        for(var item:cases) {
            boolean valid;
            try {AnswerValidation.validate(item.selected(),List.of(candidate));valid=true;}catch(IllegalArgumentException ex){valid=false;}
            if(valid==item.valid())passed++;
        }
        double citations=(double)passed/cases.length;
        Path output=classes().getParent().resolve("ai-evaluation/results.json");Files.createDirectories(output.getParent());
        Files.writeString(output,json.writeValueAsString(Map.of("extraction_field_accuracy",accuracy,"field_checks",total,
            "prompt_injection_pass_rate",injection,"injection_cases",attacks.length,"citation_resolution",citations,"citation_cases",cases.length,
            "scope","Deterministic unit evaluation only; storage-backed citation integrity requires WorkspaceIntegrationTest and is not measured by this unit run.")));
        assertThat(accuracy).isGreaterThanOrEqualTo(thresholds.path("extraction_field_accuracy").doubleValue());
        assertThat(injection).isGreaterThanOrEqualTo(thresholds.path("prompt_injection_pass_rate").doubleValue());
        assertThat(citations).isGreaterThanOrEqualTo(thresholds.path("citation_resolution").doubleValue());
    }
    @Test void safePoliciesAreAcceptedAndBinaryDocumentsAreRejected() {
        assertThat(ContentSafety.unsafe("Synthetic policy: staff must request a supervisor review before closing a case.")).isFalse();
        assertThatThrownBy(()->ContentSafety.text(new byte[]{(byte)0xff})).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->ContentSafety.text(new byte[]{65,0})).isInstanceOf(IllegalArgumentException.class);
    }
}
