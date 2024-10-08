package com.sguchatbot.backend.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.CreatedDate;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Slf4j
public class Record {
    private static final String[] CITIZEN_ID_LABELS = {"Cmnd", "Cccd"};
    public String id;

    private String import_from;

    public String type;

    @Setter
    private String citizen_id;

    @CreatedDate
    private LocalDateTime import_date;

    private Map<String, String> data;

    public Record(String import_from, String type, Map<String, String> data) throws IOException {
        this.import_from = import_from;
        this.type = type;
        this.data = data;
        this.import_date = LocalDateTime.now();

        for (String label : CITIZEN_ID_LABELS) {
            if (data.containsKey(label)) {
                this.citizen_id = data.get(label);
                break;
            }
        }

        if (this.citizen_id == null) {
            log.info(data.toString());
            throw new IOException("No citizen ID found in data");
        }
    }

    @Override
    public String toString() {
        return "Record{" +
                "id='" + id + '\'' +
                ", file_id='" + import_from + '\'' +
                ", type='" + type + '\'' +
                ", citizen_id='" + citizen_id + '\'' +
                ", import_date=" + import_date +
                ", data=" + data +
                '}';
    }
}
