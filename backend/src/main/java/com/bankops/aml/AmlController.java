package com.bankops.aml;
import com.bankops.common.UserContext;
import com.bankops.common.PageResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
@RestController @RequestMapping("/api/v1/aml")
public class AmlController {
    private final AmlService service;
    public AmlController(AmlService service) { this.service=service; }
    @GetMapping("/alerts") public PageResponse<AmlService.Alert> list(@RequestAttribute(UserContext.REQUEST_ATTRIBUTE) UserContext user,
        @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return service.list(user,page,size); }
    @GetMapping("/cases/{caseId}/transactions") public java.util.List<AmlService.TransactionEvidence> evidence(
        @RequestAttribute(UserContext.REQUEST_ATTRIBUTE) UserContext user,@PathVariable java.util.UUID caseId) { return service.evidence(user,caseId); }
    @PostMapping("/simulations") @ResponseStatus(HttpStatus.CREATED) public AmlService.Simulation simulate(@RequestAttribute(UserContext.REQUEST_ATTRIBUTE) UserContext user) { return service.simulate(user); }
}
