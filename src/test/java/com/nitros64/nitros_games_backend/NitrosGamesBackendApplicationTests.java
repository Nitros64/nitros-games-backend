package com.nitros64.nitros_games_backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.persistence.EntityManagerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

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
		assertEquals(14, entityManagerFactory.getMetamodel().getEntities().size());
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
					.with(httpBasic("test-admin", "test-admin-password"))
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
					.with(httpBasic("test-admin", "test-admin-password"))
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
				.andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE))
				.andExpect(content().contentType("application/problem+json"))
				.andExpect(jsonPath("$.code").value("authentication_required"));
	}

	@Test
	void administratorCanExecuteMutations() throws Exception {
		mockMvc.perform(delete("/api/v1/gamegenre/9999")
					.with(httpBasic("test-admin", "test-admin-password")))
				.andExpect(status().isNoContent());
	}

	@Test
	void authenticatedUserWithoutAdminRoleCannotExecuteMutations() throws Exception {
		mockMvc.perform(delete("/api/v1/gamegenre/9999")
					.with(user("catalog-reader").roles("USER")))
				.andExpect(status().isForbidden())
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
