package com.fixflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fixflow.entity.ErrorCategory;
import com.fixflow.entity.KnowledgeArticle;
import com.fixflow.parser.ParsedLog;
import com.fixflow.repository.KnowledgeArticleRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KnowledgeBaseServiceTest {

    @Mock
    private KnowledgeArticleRepository repository;

    private final LogParserService parser = new LogParserService();
    private KnowledgeBaseService service;

    @BeforeEach
    void setUp() {
        service = new KnowledgeBaseService(repository);
    }

    private KnowledgeArticle article(String id, String title) {
        return new KnowledgeArticle(id, title, ErrorCategory.DATABASE, "keywords", "Explanation.",
                "Cause", "Step | Reason | LOW", "Verify", "Fix.", "Built-in FixFlow guide");
    }

    @Test
    void findRelevantReturnsKnownSignaturesInPriorityOrder() {
        ParsedLog parsed = parser.parse("""
                org.springframework.beans.factory.BeanCreationException: Error creating bean
                Caused by: java.net.ConnectException: Connection refused: getsockopt
                """);
        KnowledgeArticle bean = article("spring-bean-creation-failure", "Bean creation failure");
        KnowledgeArticle connection = article("db-connection-refused", "Connection refused");
        when(repository.findAllById(List.of("db-connection-refused", "spring-bean-creation-failure")))
                .thenReturn(List.of(bean, connection));
        when(repository.searchByText(anyString(), eq(3))).thenReturn(List.of());

        List<KnowledgeArticle> result = service.findRelevant(parsed);

        assertEquals(List.of("db-connection-refused", "spring-bean-creation-failure"),
                result.stream().map(KnowledgeArticle::getId).toList());
    }

    @Test
    void findRelevantReturnsNothingWhenThereIsNoEvidence() {
        List<KnowledgeArticle> result = service.findRelevant(ParsedLog.empty(1));

        assertTrue(result.isEmpty());
        verifyNoInteractions(repository);
    }

    @Test
    void fullTextSearchFillsSlotsForUnknownExceptions() {
        ParsedLog parsed = parser.parse("java.lang.ArithmeticException: / by zero");
        KnowledgeArticle first = article("a1", "First");
        KnowledgeArticle second = article("a2", "Second");
        when(repository.searchByText(anyString(), eq(3))).thenReturn(List.of(first, second));

        List<KnowledgeArticle> result = service.findRelevant(parsed);

        assertEquals(List.of("a1", "a2"), result.stream().map(KnowledgeArticle::getId).toList());
    }

    @Test
    void blankSearchListsAllArticles() {
        when(repository.findAllByOrderByTitleAsc()).thenReturn(List.of(article("a1", "First")));

        assertEquals(1, service.search("   ").size());
    }

    @Test
    void searchUsesFullTextQuery() {
        when(repository.searchByText("postgres password", 20)).thenReturn(List.of(article("a1", "First")));

        assertEquals("a1", service.search("postgres password").get(0).getId());
    }

}