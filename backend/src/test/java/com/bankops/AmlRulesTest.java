package com.bankops;
import com.bankops.aml.AmlRules;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
class AmlRulesTest {
    @Test void demoProducesOnlyExplainableSyntheticFindings() {
        var findings=AmlRules.evaluate(AmlRules.demo(Instant.parse("2026-01-01T00:00:00Z")));
        assertThat(findings).hasSize(2);
        assertThat(findings).extracting(AmlRules.Finding::code).containsExactly("LARGE_EUR_TRANSFER","RAPID_EUR_TRANSFERS");
    }
    @Test void lowAmountsOutsideVelocityWindowAndOtherCurrenciesDoNotTrigger() {
        Instant at=Instant.parse("2026-01-01T00:00:00Z");List<AmlRules.Transaction> rows=new ArrayList<>();
        for(int i=0;i<5;i++)rows.add(new AmlRules.Transaction("SYN-001",new BigDecimal("99"),"EUR",at.plusSeconds(i*660)));
        rows.add(new AmlRules.Transaction("SYN-002",new BigDecimal("50000"),"USD",at));
        assertThat(AmlRules.evaluate(rows)).isEmpty();
    }
    @Test void largeAmountThresholdIncludesBoundary() {
        var row=new AmlRules.Transaction("SYN-001",new BigDecimal("10000"),"EUR",Instant.now());
        assertThat(AmlRules.evaluate(List.of(row))).hasSize(1);
    }
}
