package com.internship.task.ride_together.controller;

import com.jayway.jsonpath.JsonPath;
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

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
		"spring.datasource.url=jdbc:h2:mem:office-test;DB_CLOSE_DELAY=-1",
		"spring.jpa.hibernate.ddl-auto=create-drop"
})
class OfficeControllerIntegrationTest {

	private static final String PASSWORD = "StrongPass1";
	private static final String UUID_PATTERN = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private EntityManager entityManager;

	@Test
	void shouldCreateOfficeWhenUserIsAdmin() throws Exception {
		MockHttpSession admin = registerAndLogin("admin", true);

		createOffice(admin, "Gdansk HQ", 54.35, 18.64)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(matchesPattern(UUID_PATTERN)))
				.andExpect(jsonPath("$.name").value("Gdansk HQ"))
				.andExpect(jsonPath("$.latitude").value(54.35))
				.andExpect(jsonPath("$.longitude").value(18.64))
				.andExpect(jsonPath("$.organisationCode").value(matchesPattern("^\\d{6}$")));
	}

	@Test
	void shouldReturnForbiddenWhenRegularUserCreatesOffice() throws Exception {
		MockHttpSession user = registerAndLogin("anna", false);

		createOffice(user, "Gdansk HQ", 54.35, 18.64).andExpect(status().isForbidden());
	}

	@Test
	void shouldReturnUnauthorizedWhenNotLoggedInCreatesOffice() throws Exception {
		createOffice(null, "Gdansk HQ", 54.35, 18.64).andExpect(status().isUnauthorized());
	}

	@Test
	void shouldReturnBadRequestWhenOfficeNameIsBlank() throws Exception {
		MockHttpSession admin = registerAndLogin("admin", true);

		createOffice(admin, "", 54.35, 18.64).andExpect(status().isBadRequest());
	}

	@Test
	void shouldReturnBadRequestWhenOfficeLatitudeIsOutOfRange() throws Exception {
		MockHttpSession admin = registerAndLogin("admin", true);

		createOffice(admin, "Gdansk HQ", 91.0, 18.64).andExpect(status().isBadRequest());
	}

	@Test
	void shouldReturnBadRequestWhenOfficeLongitudeIsOutOfRange() throws Exception {
		MockHttpSession admin = registerAndLogin("admin", true);

		createOffice(admin, "Gdansk HQ", 54.35, 181.0).andExpect(status().isBadRequest());
	}

	@Test
	void shouldJoinOfficeAndReturnItFromMe() throws Exception {
		MockHttpSession admin = registerAndLogin("admin", true);
		String code = createOfficeAndGetCode(admin, "Gdansk HQ");
		MockHttpSession user = registerAndLogin("anna", false);

		join(user, code).andExpect(status().isNoContent());

		mockMvc.perform(get("/api/offices/me").session(user))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Gdansk HQ"))
				.andExpect(jsonPath("$.organisationCode").value(code));
	}

	@Test
	void shouldReturnNotFoundWhenOrganisationCodeDoesNotExist() throws Exception {
		MockHttpSession user = registerAndLogin("anna", false);

		join(user, "000000").andExpect(status().isNotFound());
	}

	@Test
	void shouldReturnBadRequestWhenOrganisationCodeIsBlank() throws Exception {
		MockHttpSession user = registerAndLogin("anna", false);

		join(user, "").andExpect(status().isBadRequest());
	}

	@Test
	void shouldReturnBadRequestWhenOrganisationCodeHasWrongLength() throws Exception {
		MockHttpSession user = registerAndLogin("anna", false);

		join(user, "12345").andExpect(status().isBadRequest());
	}

	@Test
	void shouldReturnBadRequestWhenOrganisationCodeContainsLetters() throws Exception {
		MockHttpSession user = registerAndLogin("anna", false);

		join(user, "abcdef").andExpect(status().isBadRequest());
	}

	@Test
	void shouldChangeOfficeWhenUserJoinsAnotherOne() throws Exception {
		MockHttpSession admin = registerAndLogin("admin", true);
		String firstCode = createOfficeAndGetCode(admin, "First HQ");
		String secondCode = createOfficeAndGetCode(admin, "Second HQ");
		MockHttpSession user = registerAndLogin("anna", false);
		join(user, firstCode).andExpect(status().isNoContent());

		join(user, secondCode).andExpect(status().isNoContent());

		mockMvc.perform(get("/api/offices/me").session(user))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Second HQ"));
	}

	@Test
	void shouldReturnUnauthorizedWhenNotLoggedInJoinsOffice() throws Exception {
		join(null, "123456").andExpect(status().isUnauthorized());
	}

	@Test
	void shouldReturnNotFoundFromMeWhenUserHasNoOffice() throws Exception {
		MockHttpSession user = registerAndLogin("anna", false);

		mockMvc.perform(get("/api/offices/me").session(user))
				.andExpect(status().isNotFound());
	}

	@Test
	void shouldReturnUnauthorizedFromMeWhenNotLoggedIn() throws Exception {
		mockMvc.perform(get("/api/offices/me"))
				.andExpect(status().isUnauthorized());
	}

	private ResultActions createOffice(MockHttpSession session, String name, double latitude, double longitude)
			throws Exception {
		String body = """
				{"name": "%s", "latitude": %s, "longitude": %s}
				""".formatted(name, latitude, longitude);
		var request = post("/api/offices")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(body);
		if (session != null) {
			request.session(session);
		}
		return mockMvc.perform(request);
	}

	private String createOfficeAndGetCode(MockHttpSession admin, String name) throws Exception {
		String response = createOffice(admin, name, 54.35, 18.64)
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(response, "$.organisationCode");
	}

	private ResultActions join(MockHttpSession session, String code) throws Exception {
		String body = """
				{"organisationCode": "%s"}
				""".formatted(code);
		var request = post("/api/offices/join")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(body);
		if (session != null) {
			request.session(session);
		}
		return mockMvc.perform(request);
	}

	private MockHttpSession registerAndLogin(String username, boolean admin) throws Exception {
		String registerBody = """
				{"username": "%s", "email": "%s@example.com", "password": "%s"}
				""".formatted(username, username, PASSWORD);
		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(registerBody)).andExpect(status().isCreated());
		if (admin) {
			entityManager.flush();
			jdbcTemplate.update("UPDATE users SET role = 'ADMIN' WHERE username = ?", username);
			entityManager.clear();
		}
		String loginBody = """
				{"username": "%s", "password": "%s"}
				""".formatted(username, PASSWORD);
		var result = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginBody)).andExpect(status().isOk()).andReturn();
		return (MockHttpSession) result.getRequest().getSession(false);
	}
}
