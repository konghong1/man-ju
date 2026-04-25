package org.kh.manju.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class InternalRuleLlmClient extends SimulatedLlmClient {

    public InternalRuleLlmClient(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    public String provider() {
        return "internal";
    }
}
