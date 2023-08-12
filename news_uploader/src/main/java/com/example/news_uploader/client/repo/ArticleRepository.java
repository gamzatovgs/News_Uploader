package com.example.news_uploader.client.repo;

import com.example.news_uploader.entity.Article;

import java.util.List;

public interface ArticleRepository {
    void batchInsert(List<Article> articles);
}
