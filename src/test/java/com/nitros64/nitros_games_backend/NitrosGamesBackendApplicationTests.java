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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import org.springframework.http.HttpHeaders;

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
				.andExpect(jsonPath("$.Message").value("Entity Not Found"))
				.andExpect(jsonPath("$.Exception").value("NotFoundException"));
	}

	@Test
	void mutationsRequireAuthentication() throws Exception {
		mockMvc.perform(delete("/api/v1/gamegenre/9999"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE));
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
				.andExpect(status().isForbidden());
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
