package com.bankops.dashboard;

import com.bankops.common.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard", description = "Role-aware summary built from stored data")
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    @Operation(summary = "Dashboard summary",
        description = "Real counts plus the widgets available to the caller's roles. "
            + "Widgets for modules that do not exist yet are returned as unavailable with a reason.")
    public DashboardSummary summary(@RequestAttribute(UserContext.REQUEST_ATTRIBUTE) UserContext user) {
        return service.summary(user);
    }
}
