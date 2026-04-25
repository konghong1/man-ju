package org.kh.manju.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.kh.manju.model.GenerationStep;
import org.springframework.stereotype.Component;

@Component
public class StructuredOutputGuard {

    public GuardResult ensureValid(GenerationStep step, JsonNode output) {
        if (step == GenerationStep.S4_PANELIZE) {
            return ensurePanelize(output);
        }
        if (step == GenerationStep.S6_QUALITY_GATE) {
            return ensureQualityGate(output);
        }
        if (step == GenerationStep.S7_PERSIST_VERSION) {
            return ensurePersistVersion(output);
        }
        return new GuardResult(output, false);
    }

    private GuardResult ensurePanelize(JsonNode output) {
        ObjectNode fixed = asObject(output);
        boolean repaired = false;
        if (!fixed.has("sceneCount")) {
            fixed.put("sceneCount", 0);
            repaired = true;
        }
        if (!fixed.has("panelCount")) {
            fixed.put("panelCount", 0);
            repaired = true;
        }
        return new GuardResult(fixed, repaired);
    }

    private GuardResult ensureQualityGate(JsonNode output) {
        ObjectNode fixed = asObject(output);
        boolean repaired = false;
        if (!fixed.has("result")) {
            fixed.put("result", "pass");
            repaired = true;
        }
        if (!fixed.has("continuityScore")) {
            fixed.put("continuityScore", 0.8);
            repaired = true;
        }
        if (!fixed.has("consistencyScore")) {
            fixed.put("consistencyScore", 0.8);
            repaired = true;
        }
        return new GuardResult(fixed, repaired);
    }

    private GuardResult ensurePersistVersion(JsonNode output) {
        ObjectNode fixed = asObject(output);
        if (!fixed.has("versionId")) {
            fixed.put("versionId", "v-auto-repair");
            return new GuardResult(fixed, true);
        }
        return new GuardResult(fixed, false);
    }

    private ObjectNode asObject(JsonNode output) {
        if (output instanceof ObjectNode objectNode) {
            return objectNode.deepCopy();
        }
        return new ObjectNode(JsonNodeFactory.instance);
    }

    public record GuardResult(
            JsonNode output,
            boolean repaired
    ) {
    }
}
