package com.nitros64.nitros_games_backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.persistence.EntityManagerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

}
