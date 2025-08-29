package io.spring.application.article;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.spring.core.article.Article;
import io.spring.core.article.ArticleRepository;
import io.spring.core.user.User;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArticleCommandServiceTest {

  @Mock private ArticleRepository articleRepository;

  @InjectMocks private ArticleCommandService articleCommandService;

  private User user;
  private NewArticleParam newArticleParam;
  private UpdateArticleParam updateArticleParam;
  private Article article;

  @BeforeEach
  void setUp() {
    user = new User("test@example.com", "testuser", "password", "bio", "image");
    
    newArticleParam = NewArticleParam.builder()
        .title("Test Title")
        .description("Test Description")
        .body("Test Body")
        .tagList(Arrays.asList("java", "spring"))
        .build();
        
    updateArticleParam = new UpdateArticleParam("Updated Title", "Updated Body", "Updated Description");
    
    article = new Article("Original Title", "Original Description", "Original Body", 
                         Arrays.asList("tag1"), user.getId());
  }

  @Test
  void should_create_article_successfully() {
    Article result = articleCommandService.createArticle(newArticleParam, user);

    assertNotNull(result);
    assertEquals("Test Title", result.getTitle());
    assertEquals("Test Description", result.getDescription());
    assertEquals("Test Body", result.getBody());
    assertEquals(user.getId(), result.getUserId());
    verify(articleRepository).save(any(Article.class));
  }

  @Test
  void should_create_article_with_empty_tag_list() {
    NewArticleParam paramWithoutTags = NewArticleParam.builder()
        .title("Test Title")
        .description("Test Description")
        .body("Test Body")
        .tagList(Collections.emptyList())
        .build();

    Article result = articleCommandService.createArticle(paramWithoutTags, user);

    assertNotNull(result);
    assertTrue(result.getTags().isEmpty());
    verify(articleRepository).save(any(Article.class));
  }

  @Test
  void should_update_article_successfully() {
    Article result = articleCommandService.updateArticle(article, updateArticleParam);

    assertNotNull(result);
    assertEquals("Updated Title", result.getTitle());
    assertEquals("Updated Description", result.getDescription());
    assertEquals("Updated Body", result.getBody());
    verify(articleRepository).save(article);
  }

  @Test
  void should_update_article_with_partial_data() {
    UpdateArticleParam partialUpdate = new UpdateArticleParam("New Title", null, null);
    
    Article result = articleCommandService.updateArticle(article, partialUpdate);

    assertNotNull(result);
    assertEquals("New Title", result.getTitle());
    verify(articleRepository).save(article);
  }

  @Test
  void should_create_article_with_explicit_empty_tag_list() {
    NewArticleParam paramWithEmptyTagList = NewArticleParam.builder()
        .title("Test Title")
        .description("Test Description")
        .body("Test Body")
        .tagList(Collections.emptyList())
        .build();

    Article result = articleCommandService.createArticle(paramWithEmptyTagList, user);

    assertNotNull(result);
    assertTrue(result.getTags().isEmpty());
    verify(articleRepository).save(any(Article.class));
  }
}
