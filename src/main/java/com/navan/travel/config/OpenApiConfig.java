package com.navan.travel.config;

import com.navan.travel.domain.metadata.FlightMetadata;
import com.navan.travel.domain.metadata.HotelMetadata;
import com.navan.travel.domain.metadata.NavanFeeMetadata;
import com.navan.travel.domain.metadata.RailMetadata;
import com.navan.travel.service.rollup.FeeRollupItem;
import com.navan.travel.web.dto.ConflictProblemDetailDto;
import com.navan.travel.web.dto.ProblemDetailDto;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI travelCommerceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Navan Commerce: Travel Inventory & Invoicing API")
                        .description("""
                                High-performance Java 21 / Spring Boot 3.4 microservice for ingesting multi-modal travel transactions
                                (`flight`, `hotel`, `rail`, `navan_fee`) and generating immutable invoice snapshots with automated tax and fee rollups.
                                
                                ### Key Architectural Features:
                                - **Strict Idempotency & Concurrency:** Database-enforced uniqueness on `external_id` returning `409 Conflict` with the winning `id`.
                                - **Frozen Invoice Snapshots:** Deep copy of transactions via the Memento Pattern, isolating legal invoices from subsequent booking modifications.
                                - **Mutation Safeguards:** Any `PATCH` or `DELETE` attempt on an invoiced transaction is blocked with `409 Conflict`.
                                - **Financial Precision:** Strict use of `BigDecimal` with scale 2 and `HALF_UP` rounding.
                                - **High-Volume Protection:** Bounded pagination (default 50, max 100) with indexed query execution.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Navan Commerce Engineering")
                                .url("https://navan.com"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://navan.com")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local Development Server")
                ));
    }

    @Bean
    public OpenApiCustomizer openApiCustomizer() {
        return openApi -> {
            ModelConverters converters = ModelConverters.getInstance();
            converters.readAll(FlightMetadata.class).forEach(openApi.getComponents()::addSchemas);
            converters.readAll(HotelMetadata.class).forEach(openApi.getComponents()::addSchemas);
            converters.readAll(RailMetadata.class).forEach(openApi.getComponents()::addSchemas);
            converters.readAll(NavanFeeMetadata.class).forEach(openApi.getComponents()::addSchemas);
            converters.readAll(ProblemDetailDto.class).forEach(openApi.getComponents()::addSchemas);
            converters.readAll(ConflictProblemDetailDto.class).forEach(openApi.getComponents()::addSchemas);
            converters.readAll(FeeRollupItem.class).forEach(openApi.getComponents()::addSchemas);
        };
    }
}

