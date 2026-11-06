package com.bankops.cases;
import com.bankops.common.UserContext;
import com.bankops.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.util.UUID;
@RestController @RequestMapping("/api/v1/cases")
public class CaseController {
    private final CaseService service;
    public CaseController(CaseService service) { this.service=service; }
    @GetMapping public PageResponse<CaseService.Item> list(@RequestAttribute(UserContext.REQUEST_ATTRIBUTE) UserContext user,
        @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,@RequestParam(defaultValue="false") boolean reviewQueue) {
        return service.list(user,page,size,reviewQueue);
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) 
    public CaseService.Item create(@RequestAttribute(UserContext.REQUEST_ATTRIBUTE) UserContext user,@Valid @RequestBody CaseService.Create input) { return service.create(user,input); }
    @GetMapping("/{id}") public CaseService.Detail detail(@RequestAttribute(UserContext.REQUEST_ATTRIBUTE) UserContext user,@PathVariable UUID id) { return service.detail(user,id); }
    @PostMapping("/{id}/notes") public CaseService.Detail note(@RequestAttribute(UserContext.REQUEST_ATTRIBUTE) UserContext user,@PathVariable UUID id,@Valid @RequestBody CaseService.Change input) { return service.note(user,id,input); }
    @PostMapping("/{id}/submit") public CaseService.Detail submit(@RequestAttribute(UserContext.REQUEST_ATTRIBUTE) UserContext user,@PathVariable UUID id,@Valid @RequestBody CaseService.Change input) { return service.submit(user,id,input); }
    @PostMapping("/{id}/reviews") public CaseService.Detail review(@RequestAttribute(UserContext.REQUEST_ATTRIBUTE) UserContext user,@PathVariable UUID id,@Valid @RequestBody CaseService.Review input) { return service.review(user,id,input); }
}
