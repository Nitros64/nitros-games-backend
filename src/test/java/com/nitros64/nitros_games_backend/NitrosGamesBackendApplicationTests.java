package com.nitros64.nitros_games_backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.persistence.EntityManagerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static com.nitros64.nitros_games_backend.security.JwtTestSupport.adminJwt;
import static com.nitros64.nitros_games_backend.security.JwtTestSupport.userJwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.nitros64.nitros_games_backend.observability.RequestCorrelationFilter;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NitrosGamesBackendApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Test
	void contextLoads() {
		assertEquals(13, entityManagerFactory.getMetamodel().getEntities().size());
	}

	@Test
	void readinessHealthIsPublicAndDoesNotExposeDetails() throws Exception {
		mockMvc.perform(get("/actuator/health/readiness"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"))
				.andExpect(jsonPath("$.components").doesNotExist());
	}

	@Test
	void prometheusMetricsRequireAdministratorAuthentication() throws Exception {
		mockMvc.perform(get("/actuator/prometheus"))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(get("/actuator/prometheus")
					.with(adminJwt()))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString(
						"jvm_memory_used_bytes")));
	}

	@Test
	void validClientRequestIdIsReturnedToTheCaller() throws Exception {
		mockMvc.perform(get("/api/v1/gamegenre")
					.header(RequestCorrelationFilter.REQUEST_ID_HEADER, "client-request_123"))
				.andExpect(status().isOk())
				.andExpect(header().string(
						RequestCorrelationFilter.REQUEST_ID_HEADER,
						"client-request_123"));
	}

	@Test
	void unsafeClientRequestIdIsReplaced() throws Exception {
		mockMvc.perform(get("/api/v1/gamegenre")
					.header(RequestCorrelationFilter.REQUEST_ID_HEADER, "unsafe\nvalue"))
				.andExpect(status().isOk())
				.andExpect(header().string(
						RequestCorrelationFilter.REQUEST_ID_HEADER,
						org.hamcrest.Matchers.matchesPattern(
								"[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")));
	}

	@Test
	void emptyGenreCatalogCanBeRead() throws Exception {
		mockMvc.perform(get("/api/v1/gamegenre"))
				.andExpect(status().isOk())
				.andExpect(content().json("[]"));
	}

	@Test
	void unknownGenreUsesCurrentNotFoundContract() throws Exception {
		mockMvc.perform(get("/api/v1/gamegenre/9999"))
				.andExpect(status().isNotFound())
				.andExpect(content().contentType("application/problem+json"))
				.andExpect(jsonPath("$.title").value("Resource not found"))
				.andExpect(jsonPath("$.detail").value("Entity not found"))
				.andExpect(jsonPath("$.code").value("resource_not_found"))
				.andExpect(jsonPath("$.instance").value("/api/v1/gamegenre/9999"));
	}

	@Test
	void invalidRequestUsesValidationProblemWithoutRejectedValue() throws Exception {
		mockMvc.perform(post("/api/v1/gamegenre/add")
					.with(adminJwt())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"name\":\"12\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("Validation failed"))
				.andExpect(jsonPath("$.code").value("validation_failed"))
				.andExpect(jsonPath("$.errors[0].field").value("name"))
				.andExpect(jsonPath("$.errors[0].code").isNotEmpty())
				.andExpect(jsonPath("$.errors[0].message").isNotEmpty())
				.andExpect(jsonPath("$.errors[0].rejectedValue").doesNotExist())
				.andExpect(jsonPath("$.exception").doesNotExist())
				.andExpect(jsonPath("$.trace").doesNotExist());
	}

	@Test
	void malformedJsonDoesNotExposeParserInternals() throws Exception {
		mockMvc.perform(post("/api/v1/gamegenre/add")
					.with(adminJwt())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"name\":"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("malformed_json"))
				.andExpect(jsonPath("$.detail").value("The request body is not valid JSON"))
				.andExpect(jsonPath("$.exception").doesNotExist())
				.andExpect(jsonPath("$.trace").doesNotExist());
	}

	@Test
	void invalidPathVariableUsesTypeMismatchProblem() throws Exception {
		mockMvc.perform(get("/api/v1/gamegenre/not-a-number"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("type_mismatch"))
				.andExpect(jsonPath("$.detail").value("Parameter 'id' has an invalid type"));
	}

	@Test
	void unknownRouteUsesProblemDetails() throws Exception {
		mockMvc.perform(get("/api/v1/unknown-resource"))
				.andExpect(status().isNotFound())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("resource_not_found"))
				.andExpect(jsonPath("$.instance").value("/api/v1/unknown-resource"));
	}

	@Test
	void mutationsRequireAuthentication() throws Exception {
		mockMvc.perform(delete("/api/v1/gamegenre/9999"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string(
						HttpHeaders.WWW_AUTHENTICATE,
						org.hamcrest.Matchers.startsWith("Bearer")))
				.andExpect(content().contentType("application/problem+json"))
				.andExpect(jsonPath("$.code").value("authentication_required"));
	}

	@Test
	void malformedBearerTokenUsesAuthenticationProblemContract() throws Exception {
		mockMvc.perform(delete("/api/v1/gamegenre/9999")
					.header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string(
						HttpHeaders.WWW_AUTHENTICATE,
						org.hamcrest.Matchers.startsWith("Bearer")))
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("authentication_required"));
	}

	@Test
	void administratorCanExecuteMutations() throws Exception {
		mockMvc.perform(delete("/api/v1/gamegenre/9999")
					.with(adminJwt()))
				.andExpect(status().isNoContent());
	}

	@Test
	void authenticatedUserWithoutAdminRoleCannotExecuteMutations() throws Exception {
		mockMvc.perform(delete("/api/v1/gamegenre/9999")
					.with(userJwt()))
				.andExpect(status().isForbidden())
				.andExpect(header().string(
						HttpHeaders.WWW_AUTHENTICATE,
						org.hamcrest.Matchers.startsWith("Bearer")))
				.andExpect(content().contentType("application/problem+json"))
				.andExpect(jsonPath("$.code").value("access_denied"));
	}

	@Test
	void configuredCorsOriginCanPreflightAnAuthenticatedMutation() throws Exception {
		mockMvc.perform(options("/api/v1/gamegenre")
					.header(HttpHeaders.ORIGIN, "http://localhost:4200")
					.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "DELETE"))
				.andExpect(status().isOk())
				.andExpect(header().string(
						HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
						"http://localhost:4200"));
	}

	@Test
	void untrustedCorsOriginIsRejected() throws Exception {
		mockMvc.perform(options("/api/v1/gamegenre")
					.header(HttpHeaders.ORIGIN, "https://attacker.example")
					.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "DELETE"))
				.andExpect(status().isForbidden());
	}

}
