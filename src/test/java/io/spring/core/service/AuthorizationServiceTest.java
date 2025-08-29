package io.spring.core.service;

import static org.junit.jupiter.api.Assertions.*;

import io.spring.core.article.Article;
import io.spring.core.comment.Comment;
import io.spring.core.user.User;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthorizationServiceTest {

  private User articleAuthor;
  private User commentAuthor;
  private User otherUser;
  private Article article;
  private Comment comment;

  @BeforeEach
  void setUp() {
    articleAuthor = new User("author@example.com", "author", "password", "bio", "image");
    commentAuthor = new User("commenter@example.com", "commenter", "password", "bio", "image");
    otherUser = new User("other@example.com", "other", "password", "bio", "image");
    
    article = new Article("Title", "Description", "Body", Arrays.asList("tag"), articleAuthor.getId());
    comment = new Comment("Comment body", commentAuthor.getId(), article.getId());
  }

  @Test
  void should_allow_article_author_to_write_article() {
    boolean canWrite = AuthorizationService.canWriteArticle(articleAuthor, article);

    assertTrue(canWrite);
  }

  @Test
  void should_not_allow_non_author_to_write_article() {
    boolean canWrite = AuthorizationService.canWriteArticle(otherUser, article);

    assertFalse(canWrite);
  }

  @Test
  void should_allow_article_author_to_write_comment() {
    boolean canWrite = AuthorizationService.canWriteComment(articleAuthor, article, comment);

    assertTrue(canWrite);
  }

  @Test
  void should_allow_comment_author_to_write_comment() {
    boolean canWrite = AuthorizationService.canWriteComment(commentAuthor, article, comment);

    assertTrue(canWrite);
  }

  @Test
  void should_not_allow_other_user_to_write_comment() {
    boolean canWrite = AuthorizationService.canWriteComment(otherUser, article, comment);

    assertFalse(canWrite);
  }

  @Test
  void should_handle_null_user_ids_in_article_authorization() {
    User userWithNullId = new User("test@example.com", "test", "password", "bio", "image");
    Article articleWithNullUserId = new Article("Title", "Description", "Body", Arrays.asList("tag"), null);

    boolean canWrite = AuthorizationService.canWriteArticle(userWithNullId, articleWithNullUserId);

    assertFalse(canWrite);
  }

  @Test
  void should_handle_null_user_ids_in_comment_authorization() {
    User userWithNullId = new User("test@example.com", "test", "password", "bio", "image");
    Comment commentWithNullUserId = new Comment("Comment body", null, article.getId());

    boolean canWrite = AuthorizationService.canWriteComment(userWithNullId, article, commentWithNullUserId);

    assertFalse(canWrite);
  }
}
