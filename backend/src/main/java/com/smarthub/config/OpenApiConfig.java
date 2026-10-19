package com.smarthub.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Documentazione OpenAPI generata da springdoc.
 * Swagger UI: /swagger-ui.html — specifica JSON: /v3/api-docs.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI smartHubOpenApi(SmartThingsProperties properties) {
        return new OpenAPI()
                .info(new Info()
                        .title("Smart Hub API")
                        .version("v1")
                        .description("""
                                API interne della dashboard Smart Hub: modello normalizzato di stanze e dispositivi,
                                indipendente da SmartThings. Modalità corrente: **%s**.

                                Le chiamate leggono una cache aggiornata in polling dal backend, quindi possono essere
                                invocate spesso senza generare traffico verso SmartThings.""".formatted(
                                properties.getMode().name().toLowerCase()))
                        .license(new License().name("Uso privato")))
                .tags(List.of(
                        new Tag().name("Casa").description("Riepilogo per la home dashboard e azioni globali"),
                        new Tag().name("Stanze").description("Stanze con stato aggregato"),
                        new Tag().name("Dispositivi").description("Dispositivi normalizzati e comandi"),
                        new Tag().name("Scene").description("Scene SmartThings"),
                        new Tag().name("Sistema").description("Diagnostica della connessione SmartThings"),
                        new Tag().name("Autenticazione SmartThings").description("Flusso OAuth (solo SMARTTHINGS_AUTH=oauth)")));
    }
}
