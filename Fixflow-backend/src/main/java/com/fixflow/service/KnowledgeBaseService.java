package com.fixflow.service;

import com.fixflow.entity.KnowledgeArticle;
import com.fixflow.parser.ParsedLog;
import com.fixflow.repository.KnowledgeArticleRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeBaseService {

    static final int MAX_RELEVANT = 3;
    private static final int USER_SEARCH_LIMIT = 20;
    private static final int MAX_USER_QUERY_LENGTH = 200;
    private static final int MAX_QUERY_TERMS = 10;

    private static final Set<String> STOP_WORDS = Set.of(
            "exception", "exceptions", "error", "failed", "failure", "caused", "java", "lang", "with", "from",
            "that", "this", "have", "could", "been", "unable", "null", "name", "type", "class", "when", "into",
            "does", "than", "then", "some", "line", "info", "warn", "main", "thread");
    private static final Pattern CAMEL_SPLIT = Pattern.compile("(?<=[a-z])(?=[A-Z])");
    private static final Pattern WORD = Pattern.compile("[A-Za-z]{4,}");

    private final KnowledgeArticleRepository repository;

    public KnowledgeBaseService(KnowledgeArticleRepository repository) {
        this.repository = repository;
    }

    public List<KnowledgeArticle> listAll() {
        return repository.findAllByOrderByTitleAsc();
    }

    /** User-facing search. A blank query lists every article. */
    public List<KnowledgeArticle> search(String query) {
        if (query == null || query.isBlank()) {
            return listAll();
        }
        String trimmed = query.strip();
        if (trimmed.length() > MAX_USER_QUERY_LENGTH) {
            trimmed = trimmed.substring(0, MAX_USER_QUERY_LENGTH);
        }
        return repository.searchByText(trimmed, USER_SEARCH_LIMIT);
    }

    /** Finds guides for a parsed log: known signatures first, then full-text matches. */
    public List<KnowledgeArticle> findRelevant(ParsedLog parsed) {
        if (parsed == null || !parsed.hasEvidence()) {
            return List.of();
        }

        List<String> articleIds = ErrorSignatures.orderedPatterns(parsed.keyPatterns()).stream()
                .map(ErrorSignatures.ARTICLE_BY_PATTERN::get)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<KnowledgeArticle> result = new ArrayList<>();
        if (!articleIds.isEmpty()) {
            Map<String, KnowledgeArticle> byId = repository.findAllById(articleIds).stream()
                    .collect(Collectors.toMap(KnowledgeArticle::getId, article -> article, (a, b) -> a));
            for (String id : articleIds) {
                KnowledgeArticle article = byId.get(id);
                if (article != null) {
                    result.add(article);
                }
            }
        }

        if (result.size() < MAX_RELEVANT) {
            String query = buildTermQuery(parsed);
            if (!query.isEmpty()) {
                Set<String> present = result.stream().map(KnowledgeArticle::getId).collect(Collectors.toCollection(HashSet::new));
                for (KnowledgeArticle article : repository.searchByText(query, MAX_RELEVANT)) {
                    if (result.size() >= MAX_RELEVANT) {
                        break;
                    }
                    if (present.add(article.getId())) {
                        result.add(article);
                    }
                }
            }
        }

        return List.copyOf(result.size() > MAX_RELEVANT ? result.subList(0, MAX_RELEVANT) : result);
    }

    private String buildTermQuery(ParsedLog parsed) {
        String source = String.join(" ",
                splitClassName(parsed.rootCauseClass()),
                nullToEmpty(parsed.rootCauseMessage()),
                splitClassName(parsed.exceptionClass()),
                nullToEmpty(parsed.errorMessage()));

        Set<String> terms = new LinkedHashSet<>();
        Matcher matcher = WORD.matcher(source);
        while (matcher.find() && terms.size() < MAX_QUERY_TERMS) {
            String word = matcher.group().toLowerCase();
            if (!STOP_WORDS.contains(word)) {
                terms.add(word);
            }
        }
        return String.join(" or ", terms);
    }

    private String splitClassName(String className) {
        if (className == null) {
            return "";
        }
        String simpleName = className.substring(className.lastIndexOf('.') + 1);
        return CAMEL_SPLIT.matcher(simpleName).replaceAll(" ");
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}