package com.example.news_uploader.dao;

import com.example.news_uploader.entity.Article;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ArticleDAOImpl implements ArticleDAO {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public List<Article> getAllArticles() {
        Session session = entityManager.unwrap(Session.class);

        Query<Article> query = session.createQuery("from Article", Article.class);

        return query.getResultList();
    }

    @Override
    @Transactional
    public Article getArticleById(Long id) {
        Session session = entityManager.unwrap(Session.class);

        return session.get(Article.class, id);
    }

    @Override
    @Transactional
    public List<Article> getArticlesByNewsSite(String newsSite) {
        Session session = entityManager.unwrap(Session.class);

        Query<Article> query = session.createQuery("from Article a where a.newsSite =: newsSite", Article.class);
        query.setParameter("newsSite", newsSite);

        return query.getResultList();
    }
}
