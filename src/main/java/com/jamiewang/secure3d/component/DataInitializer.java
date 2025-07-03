package com.jamiewang.secure3d.component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jamiewang.secure3d.dto.PResMessageDTO;
import com.jamiewang.secure3d.service.IStorePResService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Data initializer for loading batch card range data into the database
 *
 * Runs after application startup to load initial card range data from JSON files.
 * Can be enabled/disabled via configuration and supports multiple data sources.
 */
@Component
@Slf4j
public class DataInitializer implements CommandLineRunner {

    @Value("${app.data.init.enabled}")
    private boolean initEnabled;

    @Value("${app.data.init.file}")
    private String dataFilePath;

    @Value("${app.data.init.clear-existing}")
    private boolean clearExisting;

    @Autowired
    private IStorePResService storePResService;

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void run(String... args) throws Exception {
        if (!initEnabled) {
            log.info("Data initialization is disabled. Set app.data.init.enabled=true to enable.");
            return;
        }

        log.info("Starting batch data initialization...");

        try {
            // Load data from JSON file
            PResMessageDTO presMessages = loadDataFromFile();

            if (presMessages == null) {
                log.warn("No data found to initialize");
                return;
            }

            // Process the data
            processBatchData(presMessages);

            log.info("Batch data initialization completed successfully");

        } catch (Exception e) {
            log.error("Failed to initialize batch data", e);
            // Don't throw exception to prevent application startup failure
        }
    }

    /**
     * Load PRes messages from JSON file
     */
    private PResMessageDTO loadDataFromFile() throws Exception {
        log.info("Loading data from file: {}", dataFilePath);

        Resource resource = resourceLoader.getResource(dataFilePath);

        if (!resource.exists()) {
            log.warn("Data file not found: {}", dataFilePath);
            return null;
        }

        try (InputStream inputStream = resource.getInputStream()) {
            // Try to load as array of PRes messages first
            try {
                PResMessageDTO messages = objectMapper.readValue(
                        inputStream, new TypeReference<PResMessageDTO>() {});
                log.info("Loaded PRes message from file");
                return messages;

            } catch (Exception e) {
                log.error("Failed to load PRes message from file", e);
                return null;
            }
        }
    }

    /**
     * Process batch data using the StorePRes service
     */
    private void processBatchData(PResMessageDTO presMessages) {
        log.info("Processing PRes messages for batch initialization");

        long startTime = System.currentTimeMillis();

        // Use the existing bulk processing from StorePResService
        var response = storePResService.processPResMessage(presMessages);

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        log.info("Batch processing completed in {} ms", duration);

        log.info("Results: {} total, {} successful, {} errors", response.getTotalProcessed(), response.getSuccessCount(), response.getErrorCount());

        if (!response.getErrors().isEmpty()) {
            log.warn("Errors during batch initialization:");
            response.getErrors().forEach(error -> log.warn("  - {}", error));
        }
    }

}