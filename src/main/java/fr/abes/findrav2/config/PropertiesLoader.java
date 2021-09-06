package fr.abes.findrav2.config;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Slf4j
public class PropertiesLoader {

    @Getter
    private final Properties configProp = new Properties();


    public PropertiesLoader(String fileName){

        log.info("Reading all properties from the file");
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(fileName +".properties");
        if ( in != null) {
            try {
                configProp.load(in);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            log.warn("Not found the file propertie => loading default file");
            InputStream in1 = this.getClass().getClassLoader().getResourceAsStream("default-req.properties");
            try {
                configProp.load(in1);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
