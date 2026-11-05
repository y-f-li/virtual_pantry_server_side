
package ch.uzh.ifi.hase.soprafs26.config;

import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class OpenAIConfig {

    @Value("${openai.api-key:}")
    private String apiKey;

    @Value("${OPENAI_SECRET_VERSION:}")
    private String secretVersion;

    private String resolveApiKey() {
        // Local development: use the existing API key configuration.
        if (apiKey != null && !apiKey.isBlank()) {
            return apiKey.trim();
        }

        // Google App Engine: read from Secret Manager.
        if (secretVersion != null && !secretVersion.isBlank()) {
            try (SecretManagerServiceClient client =
                     SecretManagerServiceClient.create()) {

                String key = client.accessSecretVersion(secretVersion)
                    .getPayload()
                    .getData()
                    .toStringUtf8()
                    .trim();

                if (key.isBlank()) {
                    throw new IllegalStateException(
                        "OpenAI secret is empty."
                    );
                }

                return key;
            } catch (Exception e) {
                throw new IllegalStateException(
                    "Failed to load OpenAI key from Secret Manager.", e
                );
            }
        }

        // Preserve existing behavior for tests without a configured key.
        return "";
    }

    @Bean
    public WebClient openAIWebClient() {
        return WebClient.builder()
            .baseUrl("https://api.openai.com/v1")
            .defaultHeader("Authorization", "Bearer " + resolveApiKey())
            .defaultHeader("Content-Type", "application/json")
            .build();
    }
}
