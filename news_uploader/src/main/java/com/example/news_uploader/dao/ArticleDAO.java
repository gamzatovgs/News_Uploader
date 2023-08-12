package com.example.news_uploader.dao;

import com.example.news_uploader.entity.Article;

import java.util.List;

public interface ArticleDAO {
    List<Article> getAllArticles();

    Article getArticleById(Long id);

    List<Article> getArticlesByNewsSite(String newsSite);
}
