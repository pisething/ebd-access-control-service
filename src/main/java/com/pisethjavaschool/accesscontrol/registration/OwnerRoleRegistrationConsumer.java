package com.pisethjavaschool.accesscontrol.registration;

import com.pisethjavaschool.accesscontrol.userrole.dto.AssignRoleByCodeRequest;
import com.pisethjavaschool.accesscontrol.userrole.facade.UserRoleFacade;
import com.pisethjavaschool.platform.events.registration.OwnerRoleAssigned;
import com.pisethjavaschool.platform.events.registration.PropertyOwnerProvisioned;
import com.pisethjavaschool.platform.events.registration.RegistrationEventTopics;
import com.pisethjavaschool.platform.outbox.R2dbcOutboxWriter;
import com.pisethjavaschool.platform.security.PlatformAuditorContext;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class OwnerRoleRegistrationConsumer {

    private static final UUID SYSTEM_AUDITOR = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final DatabaseClient databaseClient;
    private final UserRoleFacade userRoleFacade;
    private final R2dbcOutboxWriter outboxWriter;
    private final ObjectMapper objectMapper;

    @Transactional
    public Mono<Void> process(PropertyOwnerProvisioned event) {
        return Mono.defer(() -> {
            validate(event);
            Mono<Void> operation = claim(event.eventId())
                    .flatMap(claimed -> claimed ? assignAndPublish(event) : Mono.empty());
            return PlatformAuditorContext.withAuditorId(operation, SYSTEM_AUDITOR);
        });
    }

    private Mono<Boolean> claim(UUID eventId) {
        return databaseClient.sql("""
                        INSERT INTO registration_role_inbox (source_event_id)
                        VALUES (:eventId)
                        ON CONFLICT (source_event_id) DO NOTHING
                        RETURNING id
                        """)
                .bind("eventId", eventId)
                .map((row, metadata) -> row.get("id", UUID.class))
                .one()
                .map(id -> true)
                .defaultIfEmpty(false);
    }

    private Mono<Void> assignAndPublish(PropertyOwnerProvisioned event) {
        String roleCode = switch (event.businessType()) {
            case "RESTAURANT" -> "RESTAURANT_OWNER";
            case "KTV" -> "KTV_OWNER";
            default -> throw new IllegalArgumentException("Unsupported business type: " + event.businessType());
        };
        return userRoleFacade.assignRoleByCode(event.platformUserId(),
                        new AssignRoleByCodeRequest(roleCode, event.propertyOwnerId()))
                .then(outboxWriter.appendGenerated(RegistrationEventTopics.ROLE_ASSIGNED,
                        event.registrationId().toString(),
                        eventId -> objectMapper.writeValueAsString(new OwnerRoleAssigned(
                                eventId, event.registrationId(), event.platformUserId(), Instant.now()))))
                .then();
    }

    private void validate(PropertyOwnerProvisioned event) {
        if (event == null || event.eventId() == null || event.registrationId() == null
                || event.platformUserId() == null || event.propertyOwnerId() == null
                || event.businessType() == null || event.businessType().isBlank()) {
            throw new IllegalArgumentException("Provisioned owner identifiers and business type are required");
        }
        if (!event.businessType().equals("RESTAURANT") && !event.businessType().equals("KTV")) {
            throw new IllegalArgumentException("Unsupported business type: " + event.businessType());
        }
    }
}
