# betterreads-data-loader
Betterreads-data-loader is a [Spring Boot](https://spring.io/guides/gs/spring-boot) applications and is built using [Maven](https://spring.io/guides/gs/maven/), 
which is used for parsing authors and books dump file from openlibrary and save it's data on cassandra instance.

## Running betterreads-data-loader locally

```
1. git clone https://github.com/raviranjan3570/betterreads-data-loader.git
2. create your account on datastax astra for apache cassandra
3. create a database on cassandra
4. download secure-connect bundle to connect to your Astra DB database using the drivers
5. fill the details in application.yml file as per your db
6. download the authors and books dump file from openlibrary and give its location in application.yml file
7. run tha application, it will automatically save all the authors and books details into cassandra instance one by one
```
## Architecture
<img width="1042" alt="better-reads-data-loader-screenshot" src="/src/main/resources/data-loader-architecture.png">
