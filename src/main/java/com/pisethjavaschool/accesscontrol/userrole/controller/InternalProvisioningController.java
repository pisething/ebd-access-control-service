package com.pisethjavaschool.accesscontrol.userrole.controller;

import java.util.UUID;
import com.pisethjavaschool.accesscontrol.userrole.dto.AssignRoleByCodeRequest;
import com.pisethjavaschool.accesscontrol.userrole.dto.UserRoleResponse;
import com.pisethjavaschool.accesscontrol.userrole.facade.UserRoleFacade;
import com.pisethjavaschool.platform.security.PlatformAuditorContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/access-control/internal/provisioning")
public class InternalProvisioningController {
    private static final UUID SYSTEM_AUDITOR = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UserRoleFacade facade;
    @PostMapping("/users/{userId}/roles/by-code")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<UserRoleResponse> assign(@PathVariable UUID userId, @Valid @RequestBody AssignRoleByCodeRequest request) {
        return PlatformAuditorContext.withAuditorId(facade.assignRoleByCode(userId, request), SYSTEM_AUDITOR);
    }
}
