package sensor.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CsvReader {

    public static List<Map<String, Object>> readCsv(String filePath) throws Exception {
        List<Map<String, Object>> result = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String headerLine = br.readLine();
            String[] headers = headerLine.split(",");

            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                Map<String, Object> row = new HashMap<>();
                for (int i = 0; i < headers.length - 1; i++) {
                    String value = values[i].trim();
                    if (value.isEmpty()) {
                        row.put(headers[i], 0.0);
                    } else {
                        row.put(headers[i], Double.parseDouble(value));
                    }
                }
                result.add(row);
            }
        }

        return result;
    }
}