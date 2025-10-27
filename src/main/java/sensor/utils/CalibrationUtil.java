package sensor.utils;

import java.util.HashMap;
import java.util.Map;

public class CalibrationUtil {
    public static Map<String, Object> calibrate(Map<String, Object> reading, Map<String, Object> neighbor) {
        Map<String, Object> calibrated = new HashMap<>(reading);

        calibrated.put("Temperature", logAverage(reading, neighbor, "Temperature"));
        calibrated.put("Humidity", logAverage(reading, neighbor, "Humidity"));
        calibrated.put("Pressure", logAverage(reading, neighbor, "Pressure"));
        if (reading.containsKey("CO") && neighbor.containsKey("CO")) {
            calibrated.put("CO", logAverage(reading, neighbor, "CO"));
        }
        if (reading.containsKey("SO2") && neighbor.containsKey("SO2")) {
            calibrated.put("SO2", logAverage(reading, neighbor, "SO2"));
        }

        return calibrated;
    }

    private static double logAverage(Map<String, Object> reading, Map<String, Object> neighbor, String key) {
        Object val1Obj = reading.get(key);
        Object val2Obj = neighbor.get(key);

        double val1 = val1Obj instanceof Number ? ((Number) val1Obj).doubleValue()
                : val1Obj instanceof String ? Double.parseDouble((String) val1Obj) : 0.0;
        double val2 = val2Obj instanceof Number ? ((Number) val2Obj).doubleValue()
                : val2Obj instanceof String ? Double.parseDouble((String) val2Obj) : val1;

        double avg = (val1 + val2) / 2.0;

        System.out.println("Calibrating " + key + ": own=" + val1 + ", neighbor=" + val2 + " -> avg=" + avg);

        return avg;
    }


}
