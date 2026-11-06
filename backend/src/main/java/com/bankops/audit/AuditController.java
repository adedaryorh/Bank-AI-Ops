package com.bankops.audit;
import com.bankops.common.UserContext;
import com.bankops.common.PageResponse;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/audit/events")
public class AuditController {
    private final AuditQueryService service;
    public AuditController(AuditQueryService service) { this.service=service; }
    @GetMapping public PageResponse<AuditQueryService.Entry> list(@RequestAttribute(UserContext.REQUEST_ATTRIBUTE) UserContext user,
        @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return service.list(user,page,size); }
}
