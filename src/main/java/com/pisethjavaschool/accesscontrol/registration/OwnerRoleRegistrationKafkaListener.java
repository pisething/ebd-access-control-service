package com.pisethjavaschool.accesscontrol.registration;

import com.pisethjavaschool.platform.events.registration.PropertyOwnerProvisioned;
import com.pisethjavaschool.platform.events.registration.RegistrationEventTopics;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class OwnerRoleRegistrationKafkaListener {

    private final OwnerRoleRegistrationConsumer consumer;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = RegistrationEventTopics.OWNER_PROVISIONED,
            groupId = "access-control-owner-role-registration-v1")
    public void onOwnerProvisioned(String payload) {
        PropertyOwnerProvisioned event = objectMapper.readValue(payload, PropertyOwnerProvisioned.class);
        consumer.process(event).block();
    }
}