package com.pisethjavaschool.accesscontrol.userrole.facade;

import java.util.UUID;

import com.pisethjavaschool.accesscontrol.userrole.dto.AssignRoleRequest;
import com.pisethjavaschool.accesscontrol.userrole.dto.UserRoleResponse;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserRoleFacade {
    Mono<UserRoleResponse> assignRole(UUID userId, AssignRoleRequest request);
    Flux<UserRoleResponse> getUserRoles(UUID userId);
    Mono<Void> removeRole(UUID userId, UUID roleId);
}
