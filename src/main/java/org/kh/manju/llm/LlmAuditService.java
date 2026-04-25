package org.kh.manju.llm;

import org.kh.manju.model.GenerationStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.regex.Pattern;

@Component
public class LlmAuditService {

    private static final Logger log = LoggerFactory.getLogger(LlmAuditService.class);
    private static final Pattern OPENAI_KEY_PATTERN = Pattern.compile("sk-[A-Za-z0-9_-]{10,}");
    private static final Pattern BEARER_TOKEN_PATTERN = Pattern.compile("(?i)bearer\\s+[A-Za-z0-9._-]{8,}");
    private static final Pattern PASSWORD_FIELD_PATTERN = Pattern.compile("(?i)\"(password|secret|apiKey|token)\"\\s*:\\s*\"[^\"]*\"");

    public void recordAttempt(
            GenerationStep step,
            String provider,
            String model,
            String inputPayload,
            boolean success,
            String error
    ) {
        String redactedInput = redact(inputPayload);
        String redactedError = redact(error == null ? "" : error);
        log.info(
                "llm-audit at={} step={} provider={} model={} success={} input={} error={}",
                Instant.now(),
                step.name(),
                provider,
                model,
                success,
                redactedInput,
                redactedError
        );
    }

    String redact(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String redacted = OPENAI_KEY_PATTERN.matcher(raw).replaceAll("[REDACTED_OPENAI_KEY]");
        redacted = BEARER_TOKEN_PATTERN.matcher(redacted).replaceAll("Bearer [REDACTED_TOKEN]");
        redacted = PASSWORD_FIELD_PATTERN.matcher(redacted).replaceAll("\"$1\":\"[REDACTED]\"");
        if (redacted.length() > 800) {
            return redacted.substring(0, 800) + "...(truncated)";
        }
        return redacted;
    }
}
