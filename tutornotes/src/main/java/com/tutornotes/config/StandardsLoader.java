package com.tutornotes.config;

import com.tutornotes.model.Standard;
import com.tutornotes.repository.StandardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StandardsLoader implements CommandLineRunner {

    private final StandardRepository standardRepository;

    @Override
    public void run(String... args) throws Exception {
        if (standardRepository.count() > 0) {
            log.info("Standards already loaded ({} rows) — skipping CSV import",
                     standardRepository.count());
            return;
        }

        log.info("Loading standards from Math_Standards.csv...");
        ClassPathResource resource = new ClassPathResource("Math_Standards.csv");

        try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader()
                                                 .withTrim()
                                                 .parse(reader)) {

            List<Standard> batch = new ArrayList<>();

            for (CSVRecord record : parser) {
                String state = record.get("State");
                String grade = record.get("Grade");
                String code  = record.get("Code");
                String desc  = record.get("Description");

                // Skip the "No Standard Taught" placeholder row
                if ("No Standard Taught".equalsIgnoreCase(state) || "NA".equalsIgnoreCase(code)) {
                    continue;
                }

                Standard s = new Standard();
                s.setState(state);
                s.setGrade(grade);
                s.setCode(code);
                s.setDescription(desc);
                batch.add(s);

                if (batch.size() == 200) {          // insert in batches of 200
                    standardRepository.saveAll(batch);
                    batch.clear();
                }
            }

            if (!batch.isEmpty()) {
                standardRepository.saveAll(batch);
            }

            log.info("Loaded {} standards into DB", standardRepository.count());
        }
    }
}