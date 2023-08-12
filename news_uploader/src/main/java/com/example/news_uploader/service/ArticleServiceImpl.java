package com.example.news_uploader.service;

import com.example.news_uploader.dao.ArticleDAO;
import com.example.news_uploader.entity.Article;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ArticleServiceImpl implements ArticleService {
    @Autowired
    private ArticleDAO articleDAO;

    @Override
    public List<Article> getAllArticles() {
        return articleDAO.getAllArticles();
    }

    @Override
    public Article getArticleById(Long id) {
        return articleDAO.getArticleById(id);
    }

    @Override
    public List<Article> getArticlesByNewsSite(String newsSite) {
        return articleDAO.getArticlesByNewsSite(newsSite);
    }
}
