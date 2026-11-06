package com.bankops.policy;
import com.bankops.common.UserContext;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/policies")
public class PolicyController {
    private final PolicyService service;
    public PolicyController(PolicyService service) { this.service=service; }
    @PostMapping("/questions") public PolicyService.Answer ask(@RequestAttribute(UserContext.REQUEST_ATTRIBUTE) UserContext user,@Valid @RequestBody PolicyService.Question question) { return service.ask(user,question); }
}
