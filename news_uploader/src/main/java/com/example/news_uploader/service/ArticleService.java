package com.example.news_uploader.service;

import com.example.news_uploader.entity.Article;

import java.util.List;

public interface ArticleService {
    List<Article> getAllArticles();

    Article getArticleById(Long id);

    List<Article> getArticlesByNewsSite(String newsSite);
}
