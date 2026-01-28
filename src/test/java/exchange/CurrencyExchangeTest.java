package exchange;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CurrencyExchangeTest {

    CurrencyExchange target;
    @BeforeEach
    void setUp() {
        target = new CurrencyExchange();
    }

    @AfterEach
    void tearDown() {
        target =  null;
    }

    @Test
    void exchangeUsdToPln() throws Exception {
        //Init rates form test file
        target.infitFromFile("src/test/resources/test_rates.json");
        // Tested Operations
        double result = target.exchange("usd","pln", 100);
        // Validate results
        assertEquals( 360, result, 0.001, "Złe obliczenie ...." );
    }

    @Test
    void exchangePlnToEur() throws Exception {
        //Init rates form test file
        target.infitFromFile("src/test/resources/test_rates.json");
        // Tested Operations
        double result = target.exchange("pln","eur", 100);
        // Validate results
        assertEquals( 25, result, 0.01, "Złe obliczenie ...." );
    }

    @Test
    void exchangeUnknownCurrency() throws Exception {
        //Init rates form test file
        target.infitFromFile("src/test/resources/test_rates.json");
        // Validate and Test Exceptions
        assertThrows(Exception.class,
                () -> target.exchange("yen","eur", 100));
    }

    //Brak inicjacji stawek
    @Test
    void noInitializationOfRates() {
        // Validate and Test Exceptions
        assertThrows(CurrencyExchangeIsNotInitialized.class,
                () -> target.exchange("yen","eur", 100));
    }
    //Wymiana 0 euro na zł ?
    @Test
    void exchangeZeroValue() throws Exception {
        //Init rates form test file
        target.infitFromFile("src/test/resources/test_rates.json");
        // Tested Operations
        double result = target.exchange("pln", "eur", 0);
        // Validate results
        assertEquals(0, result, 0.01, "Złe obliczenie ....");
    }

    //Wymiana -100 euro na pln ?
    @Test
    void exchangeNegativeValue() throws Exception {
        //Init rates form test file
        target.infitFromFile("src/test/resources/test_rates.json");
        assertThrows(IllegalArgumentException.class,
                () ->target.exchange("pln", "eur", -100));
    }
}