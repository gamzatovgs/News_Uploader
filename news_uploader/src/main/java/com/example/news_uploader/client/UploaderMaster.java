package com.example.news_uploader.client;

import com.example.news_uploader.client.repo.ArticleRepository;
import com.example.news_uploader.entity.Article;
import com.example.news_uploader.entity.Response;

import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class UploaderMaster implements CommandLineRunner {
    @Autowired
    private ArticleRepository articleRepository;

    @Value("${uploaderMaster.threadPoolSize}")
    private int threadPoolSize; // Размер пула потоков

    @Value("${uploaderMaster.totalUploadingCount}")
    private int totalUploadingCount; // Общее количество скачиваемых новостных статей

    @Value("${uploderMaster.oneThreadUploadingCount}")
    private int oneThreadUploadingCount; // Количество скачиваемых новостных статей одним потоком за один цикл работы

    @Value("${uploaderMaster.newsSiteBufferLimit}")
    private int newsSiteBufferLimit; // Лимит записей новостного сайта в буфере для начала скачивания

    private final ConcurrentHashMap<String, ConcurrentSkipListMap<String, Response>> bufferGroupedByNewsSite =
            new ConcurrentHashMap<>(); // Общий буфер для накопления сгруппированных записей

    @Override
    public void run(String... args) throws Exception {
        // Загружаем черный список в память
        String blackListPath = "src/main/resources/static/blackList.txt";
        ConcurrentSkipListSet<String> blackList = new ConcurrentSkipListSet<>();

        try (BufferedReader br = new BufferedReader(new FileReader(blackListPath))) {
            br.lines().forEach(blackList::add);
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }

        // Общее количество скачанных записей
        AtomicInteger totalUploadedCount = new AtomicInteger(0);

        // Общее количество скачанных записей с учетом забаненных черным списком
        AtomicInteger totalUploadedWithBannedCount = new AtomicInteger(0);

        // Номер итерации цикла работы потоков
        AtomicInteger iterNum = new AtomicInteger(0);

        // Пул потоков
        ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);

        long startTime = new Date().getTime();

        // Итерируемся по циклу работы потоков, пока общее количество скачанных записей меньше требуемого
        while (totalUploadedCount.get() < totalUploadingCount) {
            long startIter = new Date().getTime();
            System.out.println("\nIteration number " + iterNum.get() + " started");

            // Назначаем потокам задачи на скачивание записей новостных статей
            List<Future<List<Response>>> futures = new ArrayList<>();
            for (int threadNum = 0; threadNum < threadPoolSize; threadNum++) {
                futures.add(executor.submit(
                        new Uploader(
                                oneThreadUploadingCount,
                                totalUploadedWithBannedCount.get() + threadNum * oneThreadUploadingCount
                        )
                ));
            }

            // Проходимся по потокам
            for (Future<List<Response>> future : futures) {
                try {
                    long startThread = new Date().getTime();
                    System.out.println("\nThread started");

                    // Отправляем потоку команду на скачивание записей новостных статей
                    future.get().forEach(response -> {
                        boolean banned = false;

                        // Проверяем полученную запись на вхождение в черный список
                        for (String titleWord : response.getTitle().toLowerCase().split(" ")) {
                            if (blackList.contains(titleWord)) {
                                banned = true;
                                break;
                            }
                        }

                        // Если новостная статья не забанена, ...
                        if (!banned) {
                            String newsSite = response.getNewsSite();

                            // ... то кладем ее в общий буфер для накопления,
                            // отсортировав по дате публикации и сгруппировав по новостному сайту
                            if (bufferGroupedByNewsSite.containsKey(newsSite)) {
                                bufferGroupedByNewsSite.get(newsSite).put(
                                        response.getPublishedAt(),
                                        response
                                );
                            } else {
                                ConcurrentSkipListMap<String, Response> responsesSortedByPublishedDate =
                                        new ConcurrentSkipListMap<>();

                                responsesSortedByPublishedDate.put(
                                        response.getPublishedAt(),
                                        response
                                );

                                bufferGroupedByNewsSite.put(
                                        newsSite,
                                        responsesSortedByPublishedDate
                                );
                            }

                            System.out.println(response);

                            // Если лимит накполения записей в общем буфере по новостному сайту достигнут,
                            // то начать передачу их в таблицу ARTICLES базы данных
                            int size = bufferGroupedByNewsSite.get(newsSite).size();
                            if (size >= newsSiteBufferLimit) {
                                System.out.println(size);
                                totalUploadedCount.addAndGet(clearBuffer(newsSite).get());
                                System.out.println("Downloaded " + totalUploadedCount.get() + " articles.\n");
                            }
                        }
                        totalUploadedWithBannedCount.incrementAndGet();
                    });

                    long finishThread = new Date().getTime();
                    System.out.println("Thread finished in " + (finishThread - startThread) + " ms");
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }

                // Проверка на достижение потоками требуемого количества записей для скачивания
                if (totalUploadedCount.get() >= totalUploadingCount) {
                    break;
                }
            }

            System.out.println("Past " + (new Date().getTime() - startIter) + " ms");
            System.out.println("Waiting threads for " + iterNum.get() + " iteration...");

            executor.awaitTermination(1000, TimeUnit.MILLISECONDS);

            long finishIter = new Date().getTime();
            System.out.println(
                    "Iteration number " + iterNum.get() + " finished in " + (finishIter - startIter) + " ms"
            );
            System.out.println("Downloaded " + totalUploadedCount.get() + " articles");
            System.out.println("(With banned - " + totalUploadedWithBannedCount.get() + ")");

            iterNum.incrementAndGet();
        }

        executor.awaitTermination(1000, TimeUnit.MILLISECONDS);

        System.out.println("NULL");

        // Передача оставшихся в общем буфере записей в таблицу ARTICLES базы данных
        int processedArticlesCount = clearBuffer(null).get();
        totalUploadedCount.addAndGet(processedArticlesCount);
        totalUploadedWithBannedCount.addAndGet(processedArticlesCount);

        long finishTime = new Date().getTime();
        System.out.println(
                "Uploading " + totalUploadedCount.get() + " articles finished in " + (finishTime - startTime) + " ms"
        );
        System.out.println(
                "(With banned - " + totalUploadedWithBannedCount.get() + ")\n"
        );
    }

    private AtomicInteger clearBuffer(String newsSite) {
        // Количество обработанных записей
        AtomicInteger processedArticlesCount = new AtomicInteger(0);

        long start = new Date().getTime();

        System.out.println("\nClearing buffer...");

        // Проходимся по общему буферу ...
        for (
            ConcurrentSkipListMap<String, Response>
                responsesSortedByPublishedDate :
                newsSite != null ?
                        Collections.singletonList(bufferGroupedByNewsSite.get(newsSite)) :
                        bufferGroupedByNewsSite.values()
        ) {
            // ... и собираем список записей для вставки его в таблицу ARTICLES базы данных
            List<Article> articles = new ArrayList<>();
            responsesSortedByPublishedDate.values().forEach(value -> {
                System.out.println("Starting foreach...");
                Article article = new Article();

                article.setId(value.getId());
                article.setTitle(value.getTitle());
                article.setNewsSite(value.getNewsSite());
                article.setPublishedDate(value.getPublishedAt());

                System.out.println("Past " + (new Date().getTime() - start) + " ms. Downloading URL...");
                try {
                    article.setArticle(Jsoup.connect(value.getUrl()).get().html());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                System.out.println("Past " + (new Date().getTime() - start) + " ms. Adding Article entity to batch...");
                articles.add(article);

                processedArticlesCount.incrementAndGet();
            });

            // Если лимит на количество записей по новостному сайту в общем буфере достигнут,
            // то удаляем их оттуда после обработки
            if (newsSite != null) {
                System.out.println("Removing newsSite...");
                bufferGroupedByNewsSite.remove(newsSite);
            }

            // Вставка собранного списка записей в таблицу ARTICLES базы данных батчем
            articleRepository.batchInsert(articles);
        }

        System.out.println("Past " + (new Date().getTime() - start) + " ms");

        long finish = new Date().getTime();

        System.out.println("Buffer cleared in " + (finish - start) + " ms\n");

        return processedArticlesCount;
    }
}
