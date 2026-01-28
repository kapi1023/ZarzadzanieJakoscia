package exchange;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class CurrencyExchange {

    private List<Rate> rates = null;

    public void infitFromFile(String fileName) throws IOException {
        String content = Files.readString(Paths.get(fileName), StandardCharsets.UTF_8);
        ObjectMapper objectMapper = new ObjectMapper();
        rates = objectMapper.readValue(content, new TypeReference<List<Rate>>(){});
    }

    private double rate(String from, String to) throws Exception{
        if (rates == null) throw new CurrencyExchangeIsNotInitialized();
        for (Rate rate : rates) {
            if (rate.getFrom().equals(from) && rate.getTo().equals(to)) { return rate.getRate(); }
            if (rate.getFrom().equals(to) && rate.getTo().equals(from)) { return rate.getReverse(); }
        }
        throw new Exception("Unknown currency from " + from + " to " + to);
    }

    public double exchange(String from, String to, double value) throws Exception {
        if (value < 0) throw new IllegalArgumentException("Value must be positive");
        double rate = rate(from,to);
        return rate*value;
    }
}
