package com.internship.task.ride_together.service;

import com.internship.task.ride_together.dto.RegisterRequest;
import com.internship.task.ride_together.dto.UserResponse;
import com.internship.task.ride_together.entity.User;
import com.internship.task.ride_together.exception.BadRequestException;
import com.internship.task.ride_together.exception.ConflictException;
import com.internship.task.ride_together.exception.ResourceNotFoundException;
import com.internship.task.ride_together.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceUnitTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@InjectMocks
	private UserService userService;

	@Test
	void shouldRegisterUserWithLowercaseEmailAndEncodedPassword() {
		RegisterRequest request = new RegisterRequest("John", "JohnDoe@Gmail.com", "Test123@.!");
		User savedUser = new User("John", "johndoe@gmail.com", "hashed-password");
		when(userRepository.existsByUsername("John")).thenReturn(false);
		when(userRepository.existsByEmail("johndoe@gmail.com")).thenReturn(false);
		when(passwordEncoder.encode("Test123@.!")).thenReturn("hashed-password");
		when(userRepository.save(any(User.class))).thenReturn(savedUser);

		UserResponse response = userService.register(request);

		assertEquals("John", response.username());
		assertEquals("johndoe@gmail.com", response.email());
		verify(userRepository).existsByEmail("johndoe@gmail.com");
		verify(passwordEncoder).encode("Test123@.!");
		verify(userRepository).save(any(User.class));
	}

	@Test
	void shouldThrowConflictWhenUsernameAlreadyExists() {
		RegisterRequest request = new RegisterRequest("John", "johndoe@gmail.com", "Test123@.!");
		when(userRepository.existsByUsername("John")).thenReturn(true);

		assertThrows(ConflictException.class, () -> userService.register(request));

		verify(userRepository, never()).save(any(User.class));
		verifyNoInteractions(passwordEncoder);
	}

	@Test
	void shouldThrowConflictWhenEmailAlreadyExists() {
		RegisterRequest request = new RegisterRequest("John", "johndoe@gmail.com", "Test123@.!");
		when(userRepository.existsByUsername("John")).thenReturn(false);
		when(userRepository.existsByEmail("johndoe@gmail.com")).thenReturn(true);

		assertThrows(ConflictException.class, () -> userService.register(request));

		verify(userRepository, never()).save(any(User.class));
		verifyNoInteractions(passwordEncoder);
	}

	@Test
	void shouldThrowBadRequestWhenPasswordExceeds72Bytes() {
		RegisterRequest request = new RegisterRequest("John", "johndoe@gmail.com", "ą".repeat(40));

		assertThrows(BadRequestException.class, () -> userService.register(request));

		verifyNoInteractions(userRepository, passwordEncoder);
	}

	@Test
	void shouldAcceptPasswordOfExactly72Bytes() {
		String password = "a".repeat(72);
		RegisterRequest request = new RegisterRequest("John", "johndoe@gmail.com", password);
		User savedUser = new User("John", "johndoe@gmail.com", "hashed-password");
		when(userRepository.existsByUsername("John")).thenReturn(false);
		when(userRepository.existsByEmail("johndoe@gmail.com")).thenReturn(false);
		when(passwordEncoder.encode(password)).thenReturn("hashed-password");
		when(userRepository.save(any(User.class))).thenReturn(savedUser);

		UserResponse response = userService.register(request);

		assertEquals("John", response.username());
		verify(userRepository).save(any(User.class));
	}

	@Test
	void shouldReturnProfileWhenUserExists() {
		User user = new User("John", "johndoe@gmail.com", "hashed-password");
		when(userRepository.findByUsername("John")).thenReturn(Optional.of(user));

		UserResponse response = userService.getProfile("John");

		assertEquals("John", response.username());
		assertEquals("johndoe@gmail.com", response.email());
	}

	@Test
	void shouldThrowNotFoundWhenUserDoesNotExist() {
		when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> userService.getProfile("nobody"));
	}
}
