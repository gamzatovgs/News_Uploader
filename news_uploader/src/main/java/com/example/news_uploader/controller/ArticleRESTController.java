package com.example.news_uploader.controller;

import com.example.news_uploader.entity.Article;
import com.example.news_uploader.service.ArticleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api")
public class ArticleRESTController {
    @Autowired
    private ArticleService articleService;

    @GetMapping("/articles/all")
    public List<Article> getAllArticles() {
        return articleService.getAllArticles();
    }

    @GetMapping("/articles/{id}")
    public Article getArticleById(@PathVariable Long id) {
        return articleService.getArticleById(id);
    }

    @GetMapping("/articles") // /articles?news-site=...
    public List<Article> getArticlesByNewsSite(@RequestParam(value="news-site") String newsSite) {
        return articleService.getArticlesByNewsSite(newsSite);
    }
}
