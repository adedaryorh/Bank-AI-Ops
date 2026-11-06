package com.bankops.ai;
import com.bankops.common.UserContext;
import com.bankops.common.PageResponse;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/ai/executions")
public class AiController {
    private final AiExecutionService service;
    public AiController(AiExecutionService service) { this.service=service; }
    @GetMapping public PageResponse<AiExecutionService.Execution> list(@RequestAttribute(UserContext.REQUEST_ATTRIBUTE) UserContext user,
        @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return service.list(user,page,size); }
}
