package org.kh.manju.llm;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.kh.manju.model.GenerationStep;

import static org.assertj.core.api.Assertions.assertThat;

class StructuredOutputGuardTest {

    private final StructuredOutputGuard guard = new StructuredOutputGuard();

    @Test
    void shouldRepairMissingQualityFields() {
        ObjectNode broken = JsonNodeFactory.instance.objectNode();
        broken.put("result", "pass");

        StructuredOutputGuard.GuardResult result = guard.ensureValid(GenerationStep.S6_QUALITY_GATE, broken);

        assertThat(result.repaired()).isTrue();
        assertThat(result.output().get("result").asText()).isEqualTo("pass");
        assertThat(result.output().get("continuityScore").asDouble()).isGreaterThan(0);
        assertThat(result.output().get("consistencyScore").asDouble()).isGreaterThan(0);
    }

    @Test
    void shouldKeepValidVersionPayload() {
        ObjectNode valid = JsonNodeFactory.instance.objectNode();
        valid.put("versionId", "v-001");

        StructuredOutputGuard.GuardResult result = guard.ensureValid(GenerationStep.S7_PERSIST_VERSION, valid);

        assertThat(result.repaired()).isFalse();
        assertThat(result.output().get("versionId").asText()).isEqualTo("v-001");
    }
}
