package io.spring.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import graphql.execution.DataFetcherResult;
import io.spring.api.exception.NoAuthorizationException;
import io.spring.api.exception.ResourceNotFoundException;
import io.spring.application.article.ArticleCommandService;
import io.spring.application.article.NewArticleParam;
import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.favorite.ArticleFavorite;
import io.spring.core.favorite.ArticleFavoriteRepository;
import io.spring.core.user.User;
import io.spring.graphql.exception.AuthenticationException;
import io.spring.graphql.types.ArticlePayload;
import io.spring.graphql.types.CreateArticleInput;
import io.spring.graphql.types.UpdateArticleInput;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class ArticleMutationTest {

  @Mock private ArticleCommandService articleCommandService;
  @Mock private ArticleFavoriteRepository articleFavoriteRepository;
  @Mock private ArticleRepository articleRepository;

  @InjectMocks private ArticleMutation articleMutation;

  private User user;
  private Article article;
  private CreateArticleInput createArticleInput;

  @BeforeEach
  void setUp() {
    user = new User("test@example.com", "testuser", "password", "bio", "image");
    article = new Article("Test Title", "Test Description", "Test Body", Arrays.asList("java"), user.getId());
    
    createArticleInput = CreateArticleInput.newBuilder()
        .title("Test Title")
        .description("Test Description")
        .body("Test Body")
        .tagList(Arrays.asList("java", "spring"))
        .build();
  }

  @Test
  void should_create_article_successfully() {
    SecurityContext securityContext = mock(SecurityContext.class);
    Authentication authentication = new UsernamePasswordAuthenticationToken(user, null);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);
    
    when(articleCommandService.createArticle(any(NewArticleParam.class), eq(user))).thenReturn(article);

    DataFetcherResult<ArticlePayload> result = articleMutation.createArticle(createArticleInput);

    assertNotNull(result);
    assertNotNull(result.getData());
    assertEquals(article, result.getLocalContext());
    verify(articleCommandService).createArticle(any(NewArticleParam.class), eq(user));
    
    SecurityContextHolder.clearContext();
  }

  @Test
  void should_throw_authentication_exception_when_user_not_authenticated() {
    SecurityContext securityContext = mock(SecurityContext.class);
    AnonymousAuthenticationToken anonymousAuth = mock(AnonymousAuthenticationToken.class);
    when(securityContext.getAuthentication()).thenReturn(anonymousAuth);
    SecurityContextHolder.setContext(securityContext);

    assertThrows(AuthenticationException.class, () -> {
      articleMutation.createArticle(createArticleInput);
    });
    
    SecurityContextHolder.clearContext();
  }

  @Test
  void should_create_article_with_null_tag_list() {
    CreateArticleInput inputWithNullTags = CreateArticleInput.newBuilder()
        .title("Test Title")
        .description("Test Description")
        .body("Test Body")
        .tagList(null)
        .build();
    
    SecurityContext securityContext = mock(SecurityContext.class);
    Authentication authentication = new UsernamePasswordAuthenticationToken(user, null);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);
    
    when(articleCommandService.createArticle(any(NewArticleParam.class), eq(user))).thenReturn(article);

    DataFetcherResult<ArticlePayload> result = articleMutation.createArticle(inputWithNullTags);

    assertNotNull(result);
    verify(articleCommandService).createArticle(any(NewArticleParam.class), eq(user));
    
    SecurityContextHolder.clearContext();
  }

  @Test
  void should_update_article_successfully() {
    UpdateArticleInput updateInput = UpdateArticleInput.newBuilder()
        .title("Updated Title")
        .body("Updated Body")
        .description("Updated Description")
        .build();
    
    SecurityContext securityContext = mock(SecurityContext.class);
    Authentication authentication = new UsernamePasswordAuthenticationToken(user, null);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);
    
    when(articleRepository.findBySlug("test-slug")).thenReturn(Optional.of(article));
    when(articleCommandService.updateArticle(eq(article), any())).thenReturn(article);

    DataFetcherResult<ArticlePayload> result = articleMutation.updateArticle("test-slug", updateInput);

    assertNotNull(result);
    verify(articleCommandService).updateArticle(eq(article), any());
    
    SecurityContextHolder.clearContext();
  }

  @Test
  void should_throw_no_authorization_when_updating_others_article() {
    User otherUser = new User("other@example.com", "other", "password", "bio", "image");
    UpdateArticleInput updateInput = UpdateArticleInput.newBuilder()
        .title("Updated Title")
        .build();
    
    SecurityContext securityContext = mock(SecurityContext.class);
    Authentication authentication = new UsernamePasswordAuthenticationToken(otherUser, null);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);
    
    when(articleRepository.findBySlug("test-slug")).thenReturn(Optional.of(article));

    assertThrows(NoAuthorizationException.class, () -> {
      articleMutation.updateArticle("test-slug", updateInput);
    });
    
    SecurityContextHolder.clearContext();
  }

  @Test
  void should_favorite_article_successfully() {
    SecurityContext securityContext = mock(SecurityContext.class);
    Authentication authentication = new UsernamePasswordAuthenticationToken(user, null);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);
    
    when(articleRepository.findBySlug("test-slug")).thenReturn(Optional.of(article));

    DataFetcherResult<ArticlePayload> result = articleMutation.favoriteArticle("test-slug");

    assertNotNull(result);
    verify(articleFavoriteRepository).save(any(ArticleFavorite.class));
    
    SecurityContextHolder.clearContext();
  }

  @Test
  void should_throw_resource_not_found_when_article_not_exists() {
    SecurityContext securityContext = mock(SecurityContext.class);
    Authentication authentication = new UsernamePasswordAuthenticationToken(user, null);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);
    
    when(articleRepository.findBySlug("nonexistent")).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> {
      articleMutation.favoriteArticle("nonexistent");
    });
    
    SecurityContextHolder.clearContext();
  }

  @Test
  void should_unfavorite_article_successfully() {
    ArticleFavorite favorite = new ArticleFavorite(article.getId(), user.getId());
    
    SecurityContext securityContext = mock(SecurityContext.class);
    Authentication authentication = new UsernamePasswordAuthenticationToken(user, null);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);
    
    when(articleRepository.findBySlug("test-slug")).thenReturn(Optional.of(article));
    when(articleFavoriteRepository.find(article.getId(), user.getId())).thenReturn(Optional.of(favorite));

    DataFetcherResult<ArticlePayload> result = articleMutation.unfavoriteArticle("test-slug");

    assertNotNull(result);
    verify(articleFavoriteRepository).remove(favorite);
    
    SecurityContextHolder.clearContext();
  }

  @Test
  void should_delete_article_when_authorized() {
    SecurityContext securityContext = mock(SecurityContext.class);
    Authentication authentication = new UsernamePasswordAuthenticationToken(user, null);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);
    
    when(articleRepository.findBySlug("test-slug")).thenReturn(Optional.of(article));

    var result = articleMutation.deleteArticle("test-slug");

    assertTrue(result.getSuccess());
    verify(articleRepository).remove(article);
    
    SecurityContextHolder.clearContext();
  }

  @Test
  void should_throw_no_authorization_when_deleting_others_article() {
    User otherUser = new User("other@example.com", "other", "password", "bio", "image");
    SecurityContext securityContext = mock(SecurityContext.class);
    Authentication authentication = new UsernamePasswordAuthenticationToken(otherUser, null);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    SecurityContextHolder.setContext(securityContext);
    
    when(articleRepository.findBySlug("test-slug")).thenReturn(Optional.of(article));

    assertThrows(NoAuthorizationException.class, () -> {
      articleMutation.deleteArticle("test-slug");
    });
    
    SecurityContextHolder.clearContext();
  }
}
