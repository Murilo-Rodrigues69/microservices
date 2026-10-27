package isakids.nfcelab;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class NfceLabApplication {

    private static final String APP_TIMEZONE = System.getenv().getOrDefault("APP_TIMEZONE", "America/Sao_Paulo");

    public static void main(String[] args) {
        System.setProperty("user.timezone", APP_TIMEZONE);
        TimeZone.setDefault(TimeZone.getTimeZone(APP_TIMEZONE));
        SpringApplication.run(NfceLabApplication.class, args);
    }
}
