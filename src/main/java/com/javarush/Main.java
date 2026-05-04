package com.javarush;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javarush.dao.CityDAO;
import com.javarush.dao.CountryDAO;
import com.javarush.domain.City;
import com.javarush.domain.Country;
import com.javarush.domain.CountryLanguage;
import com.javarush.redis.CityCountry;
import com.javarush.redis.Language;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisStringCommands;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;

public class Main {
    private final SessionFactory sessionFactory;
    private final RedisClient redisClient;
    private final ObjectMapper mapper;
    private final CityDAO cityDAO;
    private final CountryDAO countryDAO;

    public Main() {
        sessionFactory = prepareRelationalDb();
        cityDAO = new CityDAO(sessionFactory);
        countryDAO = new CountryDAO(sessionFactory);
        redisClient = prepareRedisClient();
        mapper = new ObjectMapper();
    }

    public static void main(String[] args) {
        Main main = new Main();
        try {
            List<City> allCities = main.fetchData();
            List<CityCountry> preparedData = main.transformData(allCities);
            main.pushToRedis(preparedData);

            // Close the current session so that the MySQL benchmark definitely goes to the DB,
            // not to Hibernate's first-level cache.
            try {
                main.sessionFactory.getCurrentSession().close();
            } catch (Exception ignored) {
                // Session can already be closed depending on previous operations.
            }

            // Existing ids in the included demo dump. With the original JavaRush/world dump,
            // these ids are also expected to exist.
            List<Integer> ids = List.of(3, 2545, 123, 4, 189, 89, 3458, 1189, 10, 102);

            long startRedis = System.currentTimeMillis();
            main.testRedisData(ids);
            long stopRedis = System.currentTimeMillis();

            long startMysql = System.currentTimeMillis();
            main.testMysqlData(ids);
            long stopMysql = System.currentTimeMillis();

            System.out.printf("%s:\t%d ms%n", "Redis", (stopRedis - startRedis));
            System.out.printf("%s:\t%d ms%n", "MySQL", (stopMysql - startMysql));
        } finally {
            main.shutdown();
        }
    }

    private SessionFactory prepareRelationalDb() {
        Properties properties = new Properties();
        properties.put(Environment.DIALECT, "org.hibernate.dialect.MySQL8Dialect");
        properties.put(Environment.DRIVER, "com.p6spy.engine.spy.P6SpyDriver");
        properties.put(Environment.URL, "jdbc:p6spy:mysql://192.168.178.48:3306/world");
        properties.put(Environment.USER, "root");
        properties.put(Environment.PASS, "root");
        properties.put(Environment.CURRENT_SESSION_CONTEXT_CLASS, "thread");
        properties.put(Environment.HBM2DDL_AUTO, "validate");
        properties.put(Environment.STATEMENT_BATCH_SIZE, "100");

        return new Configuration()
                .addAnnotatedClass(com.javarush.domain.City.class)
                .addAnnotatedClass(com.javarush.domain.Country.class)
                .addAnnotatedClass(com.javarush.domain.CountryLanguage.class)
                .addProperties(properties)
                .buildSessionFactory();
    }

    private RedisClient prepareRedisClient() {
        RedisClient client = RedisClient.create(RedisURI.create("192.168.178.48", 6379));
        try (StatefulRedisConnection<String, String> connection = client.connect()) {
            System.out.println("\nConnected to Redis\n");
        }
        return client;
    }

    private List<City> fetchData() {
        try (Session session = sessionFactory.getCurrentSession()) {
            List<City> allCities = new ArrayList<>();
            session.beginTransaction();

            // Load all countries and their languages in one query, avoiding the N+1 problem.
            List<Country> countries = countryDAO.getAll();
            System.out.printf("Loaded countries: %d%n", countries.size());

            int totalCount = cityDAO.getTotalCount();
            int step = 500;
            for (int i = 0; i < totalCount; i += step) {
                allCities.addAll(cityDAO.getItems(i, step));
            }

            session.getTransaction().commit();
            return allCities;
        }
    }

    private List<CityCountry> transformData(List<City> cities) {
        return cities.stream().map(city -> {
            CityCountry result = new CityCountry();
            result.setId(city.getId());
            result.setName(city.getName());
            result.setPopulation(city.getPopulation());
            result.setDistrict(city.getDistrict());

            Country country = city.getCountry();
            result.setAlternativeCountryCode(country.getAlternativeCode());
            result.setContinent(country.getContinent());
            result.setCountryCode(country.getCode());
            result.setCountryName(country.getName());
            result.setCountryPopulation(country.getPopulation());
            result.setCountryRegion(country.getRegion());
            result.setCountrySurfaceArea(country.getSurfaceArea());

            Set<CountryLanguage> countryLanguages = country.getLanguages();
            Set<Language> languages = countryLanguages.stream().map(countryLanguage -> {
                Language language = new Language();
                language.setLanguage(countryLanguage.getLanguage());
                language.setOfficial(countryLanguage.getOfficial());
                language.setPercentage(countryLanguage.getPercentage());
                return language;
            }).collect(Collectors.toSet());

            result.setLanguages(languages);
            return result;
        }).collect(Collectors.toList());
    }

    private void pushToRedis(List<CityCountry> data) {
        try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            RedisStringCommands<String, String> sync = connection.sync();
            for (CityCountry cityCountry : data) {
                try {
                    sync.set(String.valueOf(cityCountry.getId()), mapper.writeValueAsString(cityCountry));
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void testRedisData(List<Integer> ids) {
        try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            RedisStringCommands<String, String> sync = connection.sync();
            for (Integer id : ids) {
                String value = sync.get(String.valueOf(id));
                try {
                    mapper.readValue(value, CityCountry.class);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void testMysqlData(List<Integer> ids) {
        try (Session session = sessionFactory.getCurrentSession()) {
            session.beginTransaction();
            for (Integer id : ids) {
                City city = cityDAO.getById(id);
                Set<CountryLanguage> languages = city.getCountry().getLanguages();
                // Touch the collection to make sure the full object is loaded.
                languages.size();
            }
            session.getTransaction().commit();
        }
    }

    private void shutdown() {
        if (nonNull(sessionFactory)) {
            sessionFactory.close();
        }
        if (nonNull(redisClient)) {
            redisClient.shutdown();
        }
    }
}
