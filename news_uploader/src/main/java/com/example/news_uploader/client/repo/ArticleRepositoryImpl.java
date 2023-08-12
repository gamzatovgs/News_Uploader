package com.example.news_uploader.client.repo;

import com.example.news_uploader.entity.Article;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Repository
public class ArticleRepositoryImpl implements ArticleRepository {
    private JdbcTemplate jdbcTemplate;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public void batchInsert(List<Article> articles) {
        List<Object[]> batch = new ArrayList<>();

        System.out.println("\nBatch creating started");
        long start = new Date().getTime();

        // Создаем из полученного списка записей батч ...
        articles.forEach(a -> {
            Object[] values = new Object[] {
                    a.getId(), a.getTitle(), a.getNewsSite(), a.getPublishedDate(), a.getArticle()
            };
            batch.add(values);
        });

        System.out.println("Past " + (new Date().getTime() - start) + " ms. Starting insert...");

        // ... и вставляем его в таблицу ARTICLES базы данных
        jdbcTemplate.batchUpdate(
                "insert into articles " +
                        "(id, title, news_site, published_date, article) " +
                        "values " +
                        "(?, ?, ?, ?, ?)",
                batch
        );

        long finish = new Date().getTime();
        System.out.println("Batch inserting finished in " + (finish - start) + " ms\n");
    }
}
