package com.example.news_uploader.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;

@Data
@Entity
@Table(name = "ARTICLES")
public class Article implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    private Long id;

    private String title;

    private String newsSite;

    private String publishedDate;

    @Lob
    private String article;
}
