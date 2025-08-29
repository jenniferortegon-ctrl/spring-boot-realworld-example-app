package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import graphql.execution.DataFetcherResult;
import io.spring.api.exception.InvalidAuthenticationException;
import io.spring.application.user.RegisterParam;
import io.spring.application.user.UpdateUserCommand;
import io.spring.application.user.UserService;
import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import io.spring.graphql.types.CreateUserInput;
import io.spring.graphql.types.UpdateUserInput;
import io.spring.graphql.types.UserPayload;
import io.spring.graphql.types.UserResult;
import java.util.Optional;
import javax.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserMutationTest {

  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder encryptService;
  @Mock private UserService userService;

  @InjectMocks private UserMutation userMutation;

  private User user;
  private CreateUserInput createUserInput;

  @BeforeEach
  void setUp() {
    user = new User("test@example.com", "testuser", "password", "bio", "image");
    
    createUserInput = CreateUserInput.newBuilder()
        .email("test@example.com")
        .username("testuser")
        .password("password")
        .build();
  }

  @Test
  void should_create_user_successfully() {
    when(userService.createUser(any(RegisterParam.class))).thenReturn(user);

    DataFetcherResult<UserResult> result = userMutation.createUser(createUserInput);

    assertNotNull(result);
    assertNotNull(result.getData());
    assertEquals(user, result.getLocalContext());
    verify(userService).createUser(any(RegisterParam.class));
  }

  @Test
  void should_handle_constraint_violation_during_user_creation() {
    ConstraintViolationException exception = mock(ConstraintViolationException.class);
    when(userService.createUser(any(RegisterParam.class))).thenThrow(exception);

    DataFetcherResult<UserResult> result = userMutation.createUser(createUserInput);

    assertNotNull(result);
    assertNotNull(result.getData());
  }

  @Test
  void should_login_successfully_with_valid_credentials() {
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
    when(encryptService.matches("password", user.getPassword())).thenReturn(true);

    DataFetcherResult<UserPayload> result = userMutation.login("password", "test@example.com");

    assertNotNull(result);
    assertNotNull(result.getData());
    assertEquals(user, result.getLocalContext());
  }

  @Test
  void should_throw_invalid_authentication_with_wrong_password() {
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
    when(encryptService.matches("wrongpassword", user.getPassword())).thenReturn(false);

    assertThrows(InvalidAuthenticationException.class, () -> {
      userMutation.login("wrongpassword", "test@example.com");
    });
  }

  @Test
  void should_throw_invalid_authentication_with_nonexistent_email() {
    when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

    assertThrows(InvalidAuthenticationException.class, () -> {
      userMutation.login("password", "nonexistent@example.com");
    });
  }

  @Test
  void should_update_user_successfully() {
    SecurityContext securityContext = mock(SecurityContext.class);
    Authentication authentication = new UsernamePasswordAuthenticationToken(user, null);
    UpdateUserInput updateInput = UpdateUserInput.newBuilder()
        .email("updated@example.com")
        .username("updateduser")
        .build();

    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);

    DataFetcherResult<UserPayload> result = userMutation.updateUser(updateInput);

    assertNotNull(result);
    assertEquals(user, result.getLocalContext());
    verify(userService).updateUser(any(UpdateUserCommand.class));
    
    SecurityContextHolder.clearContext();
  }

  @Test
  void should_return_null_when_user_not_authenticated() {
    SecurityContext securityContext = mock(SecurityContext.class);
    AnonymousAuthenticationToken anonymousAuth = mock(AnonymousAuthenticationToken.class);
    UpdateUserInput updateInput = UpdateUserInput.newBuilder()
        .email("updated@example.com")
        .build();

    when(securityContext.getAuthentication()).thenReturn(anonymousAuth);
    SecurityContextHolder.setContext(securityContext);

    DataFetcherResult<UserPayload> result = userMutation.updateUser(updateInput);

    assertNull(result);
    verify(userService, never()).updateUser(any(UpdateUserCommand.class));
    
    SecurityContextHolder.clearContext();
  }

  @Test
  void should_return_null_when_principal_is_null() {
    SecurityContext securityContext = mock(SecurityContext.class);
    Authentication authentication = mock(Authentication.class);
    UpdateUserInput updateInput = UpdateUserInput.newBuilder()
        .email("updated@example.com")
        .build();

    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getPrincipal()).thenReturn(null);
    SecurityContextHolder.setContext(securityContext);

    DataFetcherResult<UserPayload> result = userMutation.updateUser(updateInput);

    assertNull(result);
    verify(userService, never()).updateUser(any(UpdateUserCommand.class));
    
    SecurityContextHolder.clearContext();
  }
}
