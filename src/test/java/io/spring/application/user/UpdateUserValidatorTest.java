package io.spring.application.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.spring.core.user.User;
import io.spring.core.user.UserRepository;
import java.util.Optional;
import javax.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpdateUserValidatorTest {

  @Mock private UserRepository userRepository;
  @Mock private ConstraintValidatorContext context;
  @Mock private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;
  @Mock private ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext nodeBuilder;

  @InjectMocks private UpdateUserValidator validator;

  private User targetUser;
  private User otherUser;
  private UpdateUserCommand command;

  @BeforeEach
  void setUp() {
    targetUser = new User("target@example.com", "target", "password", "bio", "image");
    otherUser = new User("other@example.com", "other", "password", "bio", "image");
    
    UpdateUserParam param = UpdateUserParam.builder()
        .email("newemail@example.com")
        .username("newusername")
        .build();
    command = new UpdateUserCommand(targetUser, param);
  }

  @Test
  void should_be_valid_when_email_and_username_are_available() {
    when(userRepository.findByEmail("newemail@example.com")).thenReturn(Optional.empty());
    when(userRepository.findByUsername("newusername")).thenReturn(Optional.empty());

    boolean isValid = validator.isValid(command, context);

    assertTrue(isValid);
  }

  @Test
  void should_be_valid_when_email_belongs_to_same_user() {
    when(userRepository.findByEmail("newemail@example.com")).thenReturn(Optional.of(targetUser));
    when(userRepository.findByUsername("newusername")).thenReturn(Optional.empty());

    boolean isValid = validator.isValid(command, context);

    assertTrue(isValid);
  }

  @Test
  void should_be_valid_when_username_belongs_to_same_user() {
    when(userRepository.findByEmail("newemail@example.com")).thenReturn(Optional.empty());
    when(userRepository.findByUsername("newusername")).thenReturn(Optional.of(targetUser));

    boolean isValid = validator.isValid(command, context);

    assertTrue(isValid);
  }

  @Test
  void should_be_invalid_when_email_belongs_to_different_user() {
    when(userRepository.findByEmail("newemail@example.com")).thenReturn(Optional.of(otherUser));
    when(userRepository.findByUsername("newusername")).thenReturn(Optional.empty());
    when(context.buildConstraintViolationWithTemplate("email already exist")).thenReturn(violationBuilder);
    when(violationBuilder.addPropertyNode("email")).thenReturn(nodeBuilder);

    boolean isValid = validator.isValid(command, context);

    assertFalse(isValid);
    verify(context).disableDefaultConstraintViolation();
    verify(context).buildConstraintViolationWithTemplate("email already exist");
    verify(violationBuilder).addPropertyNode("email");
    verify(nodeBuilder).addConstraintViolation();
  }

  @Test
  void should_be_invalid_when_username_belongs_to_different_user() {
    when(userRepository.findByEmail("newemail@example.com")).thenReturn(Optional.empty());
    when(userRepository.findByUsername("newusername")).thenReturn(Optional.of(otherUser));
    when(context.buildConstraintViolationWithTemplate("username already exist")).thenReturn(violationBuilder);
    when(violationBuilder.addPropertyNode("username")).thenReturn(nodeBuilder);

    boolean isValid = validator.isValid(command, context);

    assertFalse(isValid);
    verify(context).disableDefaultConstraintViolation();
    verify(context).buildConstraintViolationWithTemplate("username already exist");
    verify(violationBuilder).addPropertyNode("username");
    verify(nodeBuilder).addConstraintViolation();
  }

  @Test
  void should_be_invalid_when_both_email_and_username_belong_to_different_users() {
    ConstraintValidatorContext.ConstraintViolationBuilder emailViolationBuilder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
    ConstraintValidatorContext.ConstraintViolationBuilder usernameViolationBuilder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
    
    when(userRepository.findByEmail("newemail@example.com")).thenReturn(Optional.of(otherUser));
    when(userRepository.findByUsername("newusername")).thenReturn(Optional.of(otherUser));
    when(context.buildConstraintViolationWithTemplate("email already exist")).thenReturn(emailViolationBuilder);
    when(context.buildConstraintViolationWithTemplate("username already exist")).thenReturn(usernameViolationBuilder);
    when(emailViolationBuilder.addPropertyNode("email")).thenReturn(nodeBuilder);
    when(usernameViolationBuilder.addPropertyNode("username")).thenReturn(nodeBuilder);

    boolean isValid = validator.isValid(command, context);

    assertFalse(isValid);
    verify(context).disableDefaultConstraintViolation();
    verify(context).buildConstraintViolationWithTemplate("email already exist");
    verify(context).buildConstraintViolationWithTemplate("username already exist");
  }

  @Test
  void should_handle_null_email_in_update_param() {
    UpdateUserParam paramWithNullEmail = UpdateUserParam.builder()
        .email(null)
        .username("newusername")
        .build();
    UpdateUserCommand commandWithNullEmail = new UpdateUserCommand(targetUser, paramWithNullEmail);
    
    when(userRepository.findByEmail(null)).thenReturn(Optional.empty());
    when(userRepository.findByUsername("newusername")).thenReturn(Optional.empty());

    boolean isValid = validator.isValid(commandWithNullEmail, context);

    assertTrue(isValid);
  }

  @Test
  void should_handle_null_username_in_update_param() {
    UpdateUserParam paramWithNullUsername = UpdateUserParam.builder()
        .email("newemail@example.com")
        .username(null)
        .build();
    UpdateUserCommand commandWithNullUsername = new UpdateUserCommand(targetUser, paramWithNullUsername);
    
    when(userRepository.findByEmail("newemail@example.com")).thenReturn(Optional.empty());
    when(userRepository.findByUsername(null)).thenReturn(Optional.empty());

    boolean isValid = validator.isValid(commandWithNullUsername, context);

    assertTrue(isValid);
  }
}
