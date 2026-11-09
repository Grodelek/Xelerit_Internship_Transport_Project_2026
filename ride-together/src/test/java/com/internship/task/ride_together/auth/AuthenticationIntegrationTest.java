package com.internship.task.ride_together.auth;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
		"spring.datasource.url=jdbc:h2:mem:auth-test;DB_CLOSE_DELAY=-1",
		"spring.jpa.hibernate.ddl-auto=create-drop"
})
class AuthenticationIntegrationTest {

	private static final String PASSWORD = "StrongPass1";
	private static final String UUID_PATTERN = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private EntityManager entityManager;

	@Test
	void shouldRegisterUserAndReturnProfileWithoutPassword() throws Exception {
		register("anna", "anna@example.com", PASSWORD)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(matchesPattern(UUID_PATTERN)))
				.andExpect(jsonPath("$.username").value("anna"))
				.andExpect(jsonPath("$.email").value("anna@example.com"))
				.andExpect(jsonPath("$.password").doesNotExist());
	}

	@Test
	void shouldStorePasswordAsBcryptHash() throws Exception {
		register("anna", "anna@example.com", PASSWORD).andExpect(status().isCreated());
		entityManager.flush();

		String stored = jdbcTemplate.queryForObject(
				"SELECT password FROM users WHERE username = ?", String.class, "anna");

		assertThat(stored).isNotEqualTo(PASSWORD);
		assertThat(stored).matches("^\\$2[aby]\\$\\d{2}\\$.{53}$");
	}

	@Test
	void shouldReturnConflictWhenUsernameAlreadyExists() throws Exception {
		register("anna", "anna@example.com", PASSWORD).andExpect(status().isCreated());

		register("anna", "other@example.com", PASSWORD).andExpect(status().isConflict());
	}

	@Test
	void shouldReturnConflictWhenEmailAlreadyExists() throws Exception {
		register("anna", "anna@example.com", PASSWORD).andExpect(status().isCreated());

		register("other", "anna@example.com", PASSWORD).andExpect(status().isConflict());
	}

	@Test
	void shouldReturnBadRequestWhenUsernameIsBlank() throws Exception {
		register("", "anna@example.com", PASSWORD).andExpect(status().isBadRequest());
	}

	@Test
	void shouldReturnBadRequestWhenUsernameIsTooShort() throws Exception {
		register("ab", "anna@example.com", PASSWORD).andExpect(status().isBadRequest());
	}

	@Test
	void shouldReturnBadRequestWhenUsernameIsTooLong() throws Exception {
		register("a".repeat(31), "anna@example.com", PASSWORD).andExpect(status().isBadRequest());
	}

	@Test
	void shouldReturnBadRequestWhenEmailFormatIsInvalid() throws Exception {
		register("anna", "not-an-email", PASSWORD).andExpect(status().isBadRequest());
	}

	@Test
	void shouldReturnBadRequestWhenPasswordIsShorterThanEightCharacters() throws Exception {
		register("anna", "anna@example.com", "short12").andExpect(status().isBadRequest());
	}

	@Test
	void shouldAcceptPasswordOfExactlyEightCharacters() throws Exception {
		register("anna", "anna@example.com", "exactly8").andExpect(status().isCreated());
	}

	@Test
	void shouldLoginWithValidCredentialsAndCreateSession() throws Exception {
		register("anna", "anna@example.com", PASSWORD).andExpect(status().isCreated());

		var result = login("anna", PASSWORD)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("anna"))
				.andExpect(jsonPath("$.password").doesNotExist())
				.andReturn();

		assertThat(result.getRequest().getSession(false)).isNotNull();
	}

	@Test
	void shouldReturnUnauthorizedWhenPasswordIsWrong() throws Exception {
		register("anna", "anna@example.com", PASSWORD).andExpect(status().isCreated());

		login("anna", "WrongPass99").andExpect(status().isUnauthorized());
	}

	@Test
	void shouldReturnSameErrorForUnknownUserAndWrongPassword() throws Exception {
		register("anna", "anna@example.com", PASSWORD).andExpect(status().isCreated());

		String wrongPassword = login("anna", "WrongPass99")
				.andExpect(status().isUnauthorized())
				.andReturn().getResponse().getContentAsString();
		String unknownUser = login("nobody", "WrongPass99")
				.andExpect(status().isUnauthorized())
				.andReturn().getResponse().getContentAsString();

		assertThat(unknownUser).isEqualTo(wrongPassword);
	}

	@Test
	void shouldInvalidateSessionOnLogout() throws Exception {
		register("anna", "anna@example.com", PASSWORD).andExpect(status().isCreated());
		MockHttpSession session = loginAndGetSession("anna", PASSWORD);
		mockMvc.perform(get("/api/users/me").session(session)).andExpect(status().isOk());

		mockMvc.perform(post("/api/auth/logout").session(session))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/users/me").session(session))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void shouldReturnOwnProfileForAuthenticatedUser() throws Exception {
		register("anna", "anna@example.com", PASSWORD).andExpect(status().isCreated());
		MockHttpSession session = loginAndGetSession("anna", PASSWORD);

		mockMvc.perform(get("/api/users/me").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("anna"))
				.andExpect(jsonPath("$.email").value("anna@example.com"))
				.andExpect(jsonPath("$.password").doesNotExist());
	}

	@Test
	void shouldReturnUnauthorizedWithoutRedirectWhenNotLoggedIn() throws Exception {
		mockMvc.perform(get("/api/users/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().doesNotExist("Location"));
	}

	@Test
	void shouldShowEachUserOnlyTheirOwnProfile() throws Exception {
		register("anna", "anna@example.com", PASSWORD).andExpect(status().isCreated());
		register("bartek", "bartek@example.com", PASSWORD).andExpect(status().isCreated());
		MockHttpSession annaSession = loginAndGetSession("anna", PASSWORD);
		MockHttpSession bartekSession = loginAndGetSession("bartek", PASSWORD);

		mockMvc.perform(get("/api/users/me").session(annaSession))
				.andExpect(jsonPath("$.username").value("anna"))
				.andExpect(jsonPath("$.email").value("anna@example.com"));
		mockMvc.perform(get("/api/users/me").session(bartekSession))
				.andExpect(jsonPath("$.username").value("bartek"))
				.andExpect(jsonPath("$.email").value("bartek@example.com"));
	}

	@Test
	void shouldReturnBadRequestWhenUsernameIsTooShortAfterTrimmingWhitespace() throws Exception {
		register("ab   ", "anna@example.com", PASSWORD).andExpect(status().isBadRequest());
	}

	@Test
	void shouldReturnBadRequestWhenPasswordExceedsSeventyTwoBytes() throws Exception {
		register("anna", "anna@example.com", "ą".repeat(40)).andExpect(status().isBadRequest());
	}

	@Test
	void shouldReturnConflictWhenEmailDiffersOnlyByCase() throws Exception {
		register("anna", "anna@example.com", PASSWORD).andExpect(status().isCreated());

		register("other", "ANNA@Example.com", PASSWORD).andExpect(status().isConflict());
	}

	@Test
	void shouldReturnBadRequestWhenLoginFieldsAreMissing() throws Exception {
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void shouldReturnUnauthorizedWhenLoginPasswordExceedsSeventyTwoBytes() throws Exception {
		register("anna", "anna@example.com", PASSWORD).andExpect(status().isCreated());

		login("anna", "ą".repeat(40)).andExpect(status().isUnauthorized());
	}

	private ResultActions register(String username, String email, String password) throws Exception {
		String body = """
				{"username": "%s", "email": "%s", "password": "%s"}
				""".formatted(username, email, password);
		return mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body));
	}

	private ResultActions login(String username, String password) throws Exception {
		String body = """
				{"username": "%s", "password": "%s"}
				""".formatted(username, password);
		return mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body));
	}

	private MockHttpSession loginAndGetSession(String username, String password) throws Exception {
		var result = login(username, password).andExpect(status().isOk()).andReturn();
		return (MockHttpSession) result.getRequest().getSession(false);
	}
}
