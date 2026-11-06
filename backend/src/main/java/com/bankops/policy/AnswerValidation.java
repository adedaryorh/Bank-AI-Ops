package com.bankops.policy;
import java.util.*;
public final class AnswerValidation {
    private AnswerValidation() {}
    public static List<EvidenceSelector.Candidate> validate(List<UUID> selected,List<EvidenceSelector.Candidate> available) {
        if(selected==null || selected.size()>3 || selected.stream().anyMatch(Objects::isNull) || new HashSet<>(selected).size()!=selected.size())
            throw new IllegalArgumentException("Invalid evidence schema.");
        var byId=new HashMap<UUID,EvidenceSelector.Candidate>();available.forEach(c->byId.put(c.id(),c));
        if(selected.stream().anyMatch(id->!byId.containsKey(id))) throw new IllegalArgumentException("Unknown citation.");
        return selected.stream().map(byId::get).toList();
    }
}
