package com.example.news_uploader.entity;

import lombok.Data;

import java.util.List;

@Data
public class Response {
    Long id;

    String title;

    String url;

    String imageUrl;

    String newsSite;

    String summary;

    String publishedAt;

    String updatedAt;

    Boolean featured;

    List<Object> launches;

    List<Object> events;
}
