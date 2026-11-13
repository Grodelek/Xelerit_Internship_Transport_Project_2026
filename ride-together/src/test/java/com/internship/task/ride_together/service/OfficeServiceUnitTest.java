package com.internship.task.ride_together.service;

import com.internship.task.ride_together.dto.OfficeRequest;
import com.internship.task.ride_together.dto.OfficeResponse;
import com.internship.task.ride_together.entity.Office;
import com.internship.task.ride_together.entity.User;
import com.internship.task.ride_together.exception.ResourceNotFoundException;
import com.internship.task.ride_together.repository.OfficeRepository;
import com.internship.task.ride_together.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OfficeServiceUnitTest {

	@Mock
	private OfficeRepository officeRepository;

	@Mock
	private UserRepository userRepository;

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

	@Test
	void shouldAssignOfficeToUserWhenCodeIsCorrect() {
		Office office = new Office("Gdansk HQ", 10.5, 20.5, "123456");
		User user = createUser();
		when(userRepository.findByUsername("John")).thenReturn(Optional.of(user));
		when(officeRepository.findByOrganisationCode("123456")).thenReturn(Optional.of(office));

		officeService.joinOffice("123456", "John");

		assertEquals("Gdansk HQ", user.getOffice().getName());
	}

	@Test
	void shouldThrowNotFoundWhenOrganisationCodeIsIncorrect() {
		User user = createUser();
		when(userRepository.findByUsername("John")).thenReturn(Optional.of(user));
		when(officeRepository.findByOrganisationCode("000000")).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> officeService.joinOffice("000000", "John"));

		assertNull(user.getOffice());
	}

	@Test
	void shouldThrowNotFoundWhenUserDoesNotExist() {
		when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> officeService.joinOffice("123456", "nobody"));

		verify(officeRepository, never()).findByOrganisationCode(anyString());
	}

	@Test
	void shouldChangeOfficeWhenUserAlreadyBelongsToAnotherOffice() {
		Office oldOffice = new Office("Old HQ", 10.5, 20.5, "111111");
		Office newOffice = new Office("New HQ", 30.5, 40.5, "222222");
		User user = createUser();
		user.joinOffice(oldOffice);
		when(userRepository.findByUsername("John")).thenReturn(Optional.of(user));
		when(officeRepository.findByOrganisationCode("222222")).thenReturn(Optional.of(newOffice));

		officeService.joinOffice("222222", "John");

		assertEquals("New HQ", user.getOffice().getName());
	}

	@Test
	void shouldReturnOfficeOfUserWhenUserBelongsToOffice() {
		User user = createUser();
		user.joinOffice(new Office("Gdansk HQ", 10.5, 20.5, "123456"));
		when(userRepository.findByUsername("John")).thenReturn(Optional.of(user));

		OfficeResponse response = officeService.getOfficeOfUser("John");

		assertEquals("Gdansk HQ", response.name());
		assertEquals("123456", response.organisationCode());
	}

	@Test
	void shouldThrowNotFoundWhenUserHasNoOffice() {
		when(userRepository.findByUsername("John")).thenReturn(Optional.of(createUser()));

		assertThrows(ResourceNotFoundException.class, () -> officeService.getOfficeOfUser("John"));
	}

	@Test
	void shouldThrowNotFoundWhenUserDoesNotExistForGetOffice() {
		when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> officeService.getOfficeOfUser("nobody"));
	}

	private User createUser() {
		User user = new User("John", "john@example.com", "hashed-password");
		ReflectionTestUtils.setField(user, "carOwner", true);
		ReflectionTestUtils.setField(user, "seats", 3);
		ReflectionTestUtils.setField(user, "latitude", 54.352);
		ReflectionTestUtils.setField(user, "longitude", 18.6466);
		return user;
	}
}
