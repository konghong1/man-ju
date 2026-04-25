package org.kh.manju.llm;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LlmAuditServiceTest {

    private final LlmAuditService service = new LlmAuditService();

    @Test
    void shouldRedactSecretPatterns() {
        String raw = """
                {"apiKey":"sk-1234567890abcdef","token":"Bearer abcdefghijklmnop","password":"plain-text"}
                """;

        String redacted = service.redact(raw);
        assertThat(redacted).doesNotContain("sk-1234567890abcdef");
        assertThat(redacted).doesNotContain("abcdefghijklmnop");
        assertThat(redacted).doesNotContain("plain-text");
        assertThat(redacted).contains("[REDACTED]");
        assertThat(redacted).contains("\"password\":\"[REDACTED]\"");
    }
}
