package com.bankops.identity;

import com.bankops.common.UserContext;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "The authenticated caller")
public class AuthController {

    @GetMapping("/me")
    @Operation(summary = "Current user, roles and department",
        description = "The user is provisioned from the token on first call; provisioning is audited.")
    public MeResponse me(@RequestAttribute(UserContext.REQUEST_ATTRIBUTE) UserContext user) {
        return MeResponse.from(user);
    }
}
