package io.spring.application.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder passwordEncoder;

  private UserService userService;

  private RegisterParam registerParam;
  private User existingUser;
  private UpdateUserCommand updateUserCommand;

  @BeforeEach
  void setUp() {
    userService = new UserService(userRepository, "default-image.jpg", passwordEncoder);
    
    registerParam = new RegisterParam("test@example.com", "testuser", "password");
    existingUser = new User("existing@example.com", "existing", "encoded", "bio", "image");
    
    UpdateUserParam updateParam = UpdateUserParam.builder()
        .email("updated@example.com")
        .username("updateduser")
        .password("newpassword")
        .bio("updated bio")
        .image("updated-image.jpg")
        .build();
    updateUserCommand = new UpdateUserCommand(existingUser, updateParam);
  }

  @Test
  void should_create_user_successfully() {
    when(passwordEncoder.encode("password")).thenReturn("encoded-password");

    User result = userService.createUser(registerParam);

    assertNotNull(result);
    assertEquals("test@example.com", result.getEmail());
    assertEquals("testuser", result.getUsername());
    assertEquals("encoded-password", result.getPassword());
    assertEquals("default-image.jpg", result.getImage());
    verify(userRepository).save(any(User.class));
  }

  @Test
  void should_update_user_successfully() {
    userService.updateUser(updateUserCommand);

    assertEquals("updated@example.com", existingUser.getEmail());
    assertEquals("updateduser", existingUser.getUsername());
    assertEquals("newpassword", existingUser.getPassword());
    assertEquals("updated bio", existingUser.getBio());
    assertEquals("updated-image.jpg", existingUser.getImage());
    verify(userRepository).save(existingUser);
  }

  @Test
  void should_update_user_with_partial_data() {
    UpdateUserParam partialUpdate = UpdateUserParam.builder()
        .email("newemail@example.com")
        .build();
    UpdateUserCommand partialCommand = new UpdateUserCommand(existingUser, partialUpdate);

    userService.updateUser(partialCommand);

    assertEquals("newemail@example.com", existingUser.getEmail());
    assertEquals("existing", existingUser.getUsername());
    verify(userRepository).save(existingUser);
  }

  @Test
  void should_create_user_with_empty_bio_and_default_image() {
    when(passwordEncoder.encode("password")).thenReturn("encoded-password");

    User result = userService.createUser(registerParam);

    assertNotNull(result);
    assertEquals("", result.getBio());
    assertEquals("default-image.jpg", result.getImage());
    verify(userRepository).save(any(User.class));
  }

  @Test
  void should_update_user_with_null_values() {
    UpdateUserParam updateWithNulls = UpdateUserParam.builder()
        .email(null)
        .username(null)
        .password(null)
        .bio(null)
        .image(null)
        .build();
    UpdateUserCommand commandWithNulls = new UpdateUserCommand(existingUser, updateWithNulls);

    userService.updateUser(commandWithNulls);

    verify(userRepository).save(existingUser);
  }
}
