package com.internship.task.ride_together.service;

import com.internship.task.ride_together.dto.OfficeRequest;
import com.internship.task.ride_together.dto.OfficeResponse;
import com.internship.task.ride_together.entity.Office;
import com.internship.task.ride_together.repository.OfficeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OfficeServiceUnitTest {

	@Mock
	private OfficeRepository officeRepository;

	@InjectMocks
	private OfficeService officeService;

	@Test
	void shouldCreateOfficeWithGivenNameAndCoordinates() {
		OfficeRequest request = new OfficeRequest("Gdansk HQ", 52.23, 21.01);
		when(officeRepository.save(any(Office.class))).thenAnswer(invocation -> invocation.getArgument(0));

		OfficeResponse response = officeService.createOffice(request);

		assertEquals("Gdansk HQ", response.name());
		assertEquals(52.23, response.latitude());
		assertEquals(21.01, response.longitude());
	}

	@Test
	void shouldAlwaysGenerateSixDigitCodesIncludingLeadingZeros() {
		OfficeRequest request = new OfficeRequest("Gdansk HQ", 52.23, 21.01);
		when(officeRepository.save(any(Office.class))).thenAnswer(invocation -> invocation.getArgument(0));
		Set<String> codes = new HashSet<>();
		boolean leadingZeroSeen = false;

		for (int i = 0; i < 500; i++) {
			String code = officeService.createOffice(request).organisationCode();
			assertTrue(code.matches("\\d{6}"));
			codes.add(code);
			leadingZeroSeen = leadingZeroSeen || code.startsWith("0");
		}

		assertTrue(leadingZeroSeen);
		assertTrue(codes.size() > 400);
	}

	@Test
	void shouldKeepGeneratingCodesUntilOneIsUnique() {
		OfficeRequest request = new OfficeRequest("Gdansk HQ", 52.23, 21.01);
		when(officeRepository.existsByOrganisationCode(anyString())).thenReturn(true, true, false);
		when(officeRepository.save(any(Office.class))).thenAnswer(invocation -> invocation.getArgument(0));

		OfficeResponse response = officeService.createOffice(request);

		verify(officeRepository, times(3)).existsByOrganisationCode(anyString());
		verify(officeRepository).existsByOrganisationCode(response.organisationCode());
		verify(officeRepository).save(any(Office.class));
	}

	@Test
	void shouldReturnDataOfSavedOffice() {
		Office savedOffice = new Office("Saved name", 10.5, 20.5, "123456");
		when(officeRepository.save(any(Office.class))).thenReturn(savedOffice);

		OfficeResponse response = officeService.createOffice(new OfficeRequest("Gdansk HQ", 52.23, 21.01));

		assertEquals("Saved name", response.name());
		assertEquals("123456", response.organisationCode());
	}
}
