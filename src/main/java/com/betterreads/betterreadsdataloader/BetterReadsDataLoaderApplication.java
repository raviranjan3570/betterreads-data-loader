package com.betterreads.betterreadsdataloader;

import java.nio.file.Path;

import javax.annotation.PostConstruct;

import com.betterreads.betterreadsdataloader.author.Author;
import com.betterreads.betterreadsdataloader.author.AuthorRepository;
import com.betterreads.betterreadsdataloader.connection.DataStaxAstraProperties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.cassandra.CqlSessionBuilderCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableConfigurationProperties(DataStaxAstraProperties.class)
public class BetterReadsDataLoaderApplication {

	@Autowired
	private AuthorRepository repository;

	public static void main(String[] args) {
		SpringApplication.run(BetterReadsDataLoaderApplication.class, args);
	}

	@PostConstruct
	public void start(){
		Author author = new Author();
		author.setId("1");
		author.setName("Ravi");
		author.setPersonalName("Golu");
		repository.save(author);
	}

	@Bean
	public CqlSessionBuilderCustomizer sessionBuilderCustomizer(
		DataStaxAstraProperties properties){
			Path bundle = properties.getSecureConnectBundle().toPath();
			return builder -> builder.withCloudSecureConnectBundle(bundle);
	}
}
