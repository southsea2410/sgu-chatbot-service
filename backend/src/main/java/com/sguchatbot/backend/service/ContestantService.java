package com.sguchatbot.backend.service;

import com.sguchatbot.backend.dto.GetRecordsDto;
import com.sguchatbot.backend.entity.Contestant;
import com.sguchatbot.backend.entity.Record;
import com.sguchatbot.backend.repository.ContestantRepository;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.dhatim.fastexcel.reader.Cell;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Row;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;

@Component
@Slf4j
public class ContestantService implements Excel {

    private static final String serviceName = "contestants";
    private final Utils utils;

    private final ContestantRepository contestantRepository;
    private final MongoTemplate mongoTemplate;


    @Autowired
    public ContestantService(Utils utils, ContestantRepository contestantRepository, MongoTemplate mongoTemplate) {
        this.utils = utils;
        this.contestantRepository = contestantRepository;
        this.mongoTemplate = mongoTemplate;
    }

    public String getServiceName() {
        return serviceName;
    }

    public List<Record> readExcel(InputStream file, String fileName) throws IOException {
        List<Record> contestants = new LinkedList<>();
        LocalDateTime importTime = LocalDateTime.now();

        try (ReadableWorkbook wb = new ReadableWorkbook(file)) {
            wb.getSheets().forEach(sheet -> {
                try (Stream<Row> rows = sheet.openStream()) {
                    LinkedList<String> labels = new LinkedList<>();

                    Iterator<Row> iter = rows.iterator();

                    Row firstRow = iter.next();

                    for (Cell cell : firstRow) {
                        if (cell == null) {
                            break;
                        }
                        String converted = utils.toUpperCamelCase(cell.getRawValue());
                        labels.add(converted);
                    }

                    while (iter.hasNext()) {
                        Row r = iter.next();
                        Map<String, String> row_data = new HashMap<>();
                        boolean allNull = true;

                        Iterator<Cell> row_iter = r.iterator();

                        for (String label : labels) {
                            if (row_iter.hasNext()) {
                                Cell cell = row_iter.next();
                                String cellValue = cell.getRawValue();

                                row_data.put(label, cellValue);

                                if (cellValue != null) {
                                    allNull = false;
                                }
                            } else break;
                        }
                        if (allNull) {
                            log.info("Sheet " + sheet.getName() + ": Empty row detected, skipping...");
                            break;
                        } else {
                            Contestant contestant = new Contestant(fileName, row_data, importTime);
                            contestants.add(contestant);
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        return contestants;
    }

    public List<GetRecordsDto> getContestants() {
        // Define the group operation
        AggregationOperation operation = Aggregation.stage(new Document("$group",
                new Document("_id", "$import_from")
                        .append("count",
                                new Document("$sum", 1L))
                        .append("records",
                                new Document("$push",
                                        new Document("_id", "$$ROOT._id")
                                                .append("data", "$$ROOT.data")))));

        // Build the aggregation pipeline
        Aggregation aggregation = Aggregation.newAggregation(operation);

        // Execute the aggregation
        AggregationResults<GetRecordsDto> results = mongoTemplate.aggregate(
                aggregation, "contestants", GetRecordsDto.class);

        // Return the mapped results
        return results.getMappedResults();
    }

    public void saveRecords(List<Record> records) {
        List<Contestant> contestants = records.stream()
                .map(record -> (Contestant) record).toList();
        contestantRepository.saveAll(contestants);
    }

    public void deleteRecords(String filename) {
        contestantRepository.deleteAllByImportFrom(filename);
    }
}
