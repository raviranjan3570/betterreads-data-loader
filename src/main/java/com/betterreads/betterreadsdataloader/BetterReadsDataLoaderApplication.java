package com.betterreads.betterreadsdataloader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import com.betterreads.betterreadsdataloader.author.Author;
import com.betterreads.betterreadsdataloader.author.AuthorRepository;
import com.betterreads.betterreadsdataloader.book.Book;
import com.betterreads.betterreadsdataloader.book.BookRepository;
import com.betterreads.betterreadsdataloader.connection.DataStaxAstraProperties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.cassandra.CqlSessionBuilderCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableConfigurationProperties(DataStaxAstraProperties.class)
public class BetterReadsDataLoaderApplication {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private AuthorRepository authorRepository;

	@Autowired
	private BookRepository bookRepository;

	@Value("${datadump.location.authors}")
	private String authorsDumpLocation;

	@Value("${datadump.location.works}")
	private String worksDumpLocation;

	public static void main(String[] args) {
		SpringApplication.run(BetterReadsDataLoaderApplication.class, args);
	}

	private void initAuthors() {
		Path path = Paths.get(authorsDumpLocation);
		try (Stream<String> lines = Files.lines(path)) {
			lines.forEach(line -> {
				String jsonString = line.substring(line.indexOf("{"));
				try {
					JSONObject jsonObject = new JSONObject(jsonString);
					Author author = new Author();
					author.setId(jsonObject.optString("key").replace("/authors/", ""));
					author.setName(jsonObject.optString("name"));
					author.setPersonalName(jsonObject.optString("personal_name"));
					authorRepository.save(author);
					logger.info("Saving author -> {}", author);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void initWorks() {
		DateTimeFormatter pattern = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");
		Path path = Paths.get(worksDumpLocation);
		try (Stream<String> lines = Files.lines(path)) {
			lines.forEach(line -> {
				String jsonString = line.substring(line.indexOf("{"));
				try {
					JSONObject jsonObject = new JSONObject(jsonString);
					Book book = new Book();
					book.setId(jsonObject.getString("key").replace("/works/", ""));
					book.setName(jsonObject.optString("title"));
					JSONObject descriptionObject = jsonObject.optJSONObject("description");
					if (descriptionObject != null) {
						book.setDescription(descriptionObject.optString("value"));
					}
					JSONObject publishedObject = jsonObject.optJSONObject("created");
					if (publishedObject != null) {
						String dateString = publishedObject.optString("value");
						book.setPublishedDate(LocalDate.parse(dateString, pattern));
					}
					JSONArray coversJSONArray = jsonObject.optJSONArray("covers");
					if (coversJSONArray != null) {
						List<String> coverIds = new ArrayList<>();
						for (int i = 0; i < coversJSONArray.length(); i++) {
							coverIds.add(coversJSONArray.getString(i));
						}
						book.setCoverIds(coverIds);
					}
					JSONArray authorJSONArray = jsonObject.optJSONArray("authors");
					if (authorJSONArray != null) {
						List<String> authorIds = new ArrayList<>();
						for (int i = 0; i < authorJSONArray.length(); i++) {
							String authorId = authorJSONArray
									.getJSONObject(i)
									.getJSONObject("author")
									.getString("key")
									.replace("/authors/", "");
							authorIds.add(authorId);
						}
						book.setAuthorIds(authorIds);
						List<String> authorNames = authorIds
								.stream()
								.map(id -> authorRepository.findById(id))
								.map(optionalAuthor -> {
									if (optionalAuthor.isEmpty())
										return "Unknown Author";
									return optionalAuthor.get().getName();
								}).collect(Collectors.toList());
						book.setAuthorNames(authorNames);
					}
					bookRepository.save(book);
					logger.info("Saving book -> {}", book);
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@PostConstruct
	public void start() {
		initAuthors();
		initWorks();
	}

	@Bean
	public CqlSessionBuilderCustomizer sessionBuilderCustomizer(
			DataStaxAstraProperties properties) {
		Path bundle = properties.getSecureConnectBundle().toPath();
		return builder -> builder.withCloudSecureConnectBundle(bundle);
	}
}