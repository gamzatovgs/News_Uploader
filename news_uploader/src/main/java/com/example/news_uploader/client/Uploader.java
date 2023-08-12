package com.example.news_uploader.client;

import com.example.news_uploader.entity.Response;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.Callable;

public class Uploader implements Callable<List<Response>> {
    private final RestTemplate restTemplate = new RestTemplate();
    private final int limit;
    private final int start;

    public Uploader(int limit, int start) {
        this.limit = limit;
        this.start = start;
    }

    @Override
    public List<Response> call() throws Exception {
        // Ссылка на ресурс
        String URL = "https://api.spaceflightnewsapi.net/v3/articles?_limit=" + limit + "&_start=" + start;

        System.out.println(URL);

        // Получение GET-запросом списка записей новостных статей
        ResponseEntity<List<Response>> responseEntity = restTemplate.exchange(URL, HttpMethod.GET, null,
                        new ParameterizedTypeReference<List<Response>>() {});

        return responseEntity.getBody();
    }
}
