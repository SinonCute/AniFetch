package live.karyl.anifetch.utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class JSONToVTTConverter {

    public static String convert(String jsonContent) {
        JSONObject jsonObj = new JSONObject(jsonContent);
        JSONArray jsonArray = jsonObj.getJSONArray("body");

        StringBuilder vttContentBuilder = new StringBuilder();
        vttContentBuilder.append("WEBVTT\n");

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            String text = jsonObject.getString("content");
            double start = jsonObject.getDouble("from");
            double end = jsonObject.getDouble("to");
            LocalTime startTime = convertToTime(start);
            LocalTime endTime = convertToTime(end);

            vttContentBuilder.append(i + 1).append("\n");
            vttContentBuilder.append(formatTime(startTime)).append(" --> ").append(formatTime(endTime)).append("\n");
            vttContentBuilder.append(text).append("\n\n");
        }

        return vttContentBuilder.toString();
    }


    private static LocalTime convertToTime(double seconds) {
        int minutes = (int) (seconds / 60);
        int hours = minutes / 60;
        int secondsInt = (int) seconds % 60;
        int milliseconds = (int) ((seconds - secondsInt) * 1000);
        return LocalTime.of(hours, minutes, secondsInt, milliseconds);
    }

    private static String formatTime(LocalTime time) {
        return time.format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
    }
}
