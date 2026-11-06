package com.bankops.aml;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
public final class AmlRules {
    private AmlRules() {}
    public record Transaction(String account,BigDecimal amount,String currency,Instant at) {}
    public record Finding(String account,String code,String rationale) {}
    public static List<Finding> evaluate(List<Transaction> transactions) {
        List<Finding> findings=new ArrayList<>();
        var accounts=transactions.stream().map(Transaction::account).distinct().sorted().toList();
        for(String account:accounts) {
            var rows=transactions.stream().filter(t->account.equals(t.account()) && "EUR".equals(t.currency())).sorted(Comparator.comparing(Transaction::at)).toList();
            if(rows.stream().anyMatch(t->t.amount().compareTo(new BigDecimal("10000"))>=0))
                findings.add(new Finding(account,"LARGE_EUR_TRANSFER","Synthetic rule: at least one EUR transfer is 10,000 or more. Manual review only; this is not a regulatory threshold or evidence of wrongdoing."));
            boolean rapid=false;
            for(int i=4;i<rows.size();i++) if(Duration.between(rows.get(i-4).at(),rows.get(i).at()).compareTo(Duration.ofMinutes(10))<=0) { rapid=true;break; }
            if(rapid) findings.add(new Finding(account,"RAPID_EUR_TRANSFERS","Synthetic rule: five or more EUR transfers within ten minutes. Review context; no account or transaction action is taken."));
        }
        return List.copyOf(findings);
    }
    public static List<Transaction> demo(Instant now) {
        List<Transaction> rows=new ArrayList<>();
        rows.add(new Transaction("SYN-ACC-001",new BigDecimal("12500.00"),"EUR",now));
        for(int i=0;i<6;i++)rows.add(new Transaction("SYN-ACC-002",new BigDecimal("450.00"),"EUR",now.plusSeconds(i*60)));
        rows.add(new Transaction("SYN-ACC-003",new BigDecimal("45.00"),"EUR",now));return List.copyOf(rows);
    }
}
