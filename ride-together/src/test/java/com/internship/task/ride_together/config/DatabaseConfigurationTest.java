package com.internship.task.ride_together.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.IOException;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseConfigurationTest {

	@Test
	void shouldConfigureH2AsFileBasedDatabaseSoDataSurvivesRestart() throws IOException {
		Properties properties = loadApplicationProperties();

		assertThat(properties.getProperty("spring.datasource.url"))
				.startsWith("jdbc:h2:file:");
	}

	private Properties loadApplicationProperties() throws IOException {
		ClassPathResource config = new ClassPathResource("application.properties");
		return PropertiesLoaderUtils.loadProperties(config);
	}
}
