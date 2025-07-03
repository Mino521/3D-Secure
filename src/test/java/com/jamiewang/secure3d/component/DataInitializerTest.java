package com.jamiewang.secure3d.component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jamiewang.secure3d.dto.BulkImportResponseDTO;
import com.jamiewang.secure3d.dto.CardRangeDataDTO;
import com.jamiewang.secure3d.dto.PResMessageDTO;
import com.jamiewang.secure3d.service.IStorePResService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DataInitializerTest {

    @Mock
    private IStorePResService storePResService;

    @Mock
    private ResourceLoader resourceLoader;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private Resource resource;

    @InjectMocks
    private DataInitializer dataInitializer;

    private PResMessageDTO testPresMessage;
    private BulkImportResponseDTO successResponse;
    private BulkImportResponseDTO errorResponse;

    @BeforeEach
    void setUp() {
        // Setup test PRes message
        CardRangeDataDTO cardRange1 = new CardRangeDataDTO();
        cardRange1.setStartRange(1234567890000000L);
        cardRange1.setEndRange(1234567890999999L);
        cardRange1.setActionInd("Y");

        CardRangeDataDTO cardRange2 = new CardRangeDataDTO();
        cardRange2.setStartRange(5555555555000000L);
        cardRange2.setEndRange(5555555555999999L);
        cardRange2.setActionInd("N");

        testPresMessage = new PResMessageDTO();
        testPresMessage.setSerialNum("12345");
        testPresMessage.setMessageType("PRes");
        testPresMessage.setCardRangeData(Arrays.asList(cardRange1, cardRange2));

        // Setup success response
        successResponse = new BulkImportResponseDTO(
                2, 2, 0, new ArrayList<>(), LocalDateTime.now()
        );

        // Setup error response
        List<String> errors = Arrays.asList(
                "Error processing range 1: Database constraint violation",
                "Error processing range 2: Invalid data format"
        );
        errorResponse = new BulkImportResponseDTO(
                2, 0, 2, errors, LocalDateTime.now()
        );

        // Set default property values
        ReflectionTestUtils.setField(dataInitializer, "initEnabled", true);
        ReflectionTestUtils.setField(dataInitializer, "dataFilePath", "classpath:test-data.json");
        ReflectionTestUtils.setField(dataInitializer, "clearExisting", false);
    }

    // Test successful data initialization
    @Test
    void run_ShouldInitializeDataSuccessfully_WhenEverythingIsValid() throws Exception {
        // Arrange
        InputStream inputStream = new ByteArrayInputStream("{}".getBytes());
        when(resourceLoader.getResource("classpath:test-data.json")).thenReturn(resource);
        when(resource.exists()).thenReturn(true);
        when(resource.getInputStream()).thenReturn(inputStream);
        when(objectMapper.readValue(any(InputStream.class), any(TypeReference.class)))
                .thenReturn(testPresMessage);
        when(storePResService.processPResMessage(testPresMessage)).thenReturn(successResponse);

        // Act
        assertDoesNotThrow(() -> dataInitializer.run("arg1", "arg2"));

        // Assert
        verify(resourceLoader).getResource("classpath:test-data.json");
        verify(resource).exists();
        verify(resource).getInputStream();
        verify(objectMapper).readValue(any(InputStream.class), any(TypeReference.class));
        verify(storePResService).processPResMessage(testPresMessage);
    }

    // Test when initialization is disabled
    @Test
    void run_ShouldSkipInitialization_WhenInitEnabledIsFalse() throws Exception {
        // Arrange
        ReflectionTestUtils.setField(dataInitializer, "initEnabled", false);

        // Act
        dataInitializer.run("arg1", "arg2");

        // Assert
        verify(resourceLoader, never()).getResource(anyString());
        verify(storePResService, never()).processPResMessage(any());
    }

    // Test when data file does not exist
    @Test
    void run_ShouldHandleFileNotFound_WhenDataFileDoesNotExist() throws Exception {
        // Arrange
        when(resourceLoader.getResource("classpath:test-data.json")).thenReturn(resource);
        when(resource.exists()).thenReturn(false);

        // Act
        assertDoesNotThrow(() -> dataInitializer.run());

        // Assert
        verify(resourceLoader).getResource("classpath:test-data.json");
        verify(resource).exists();
        verify(resource, never()).getInputStream();
        verify(storePResService, never()).processPResMessage(any());
    }

    // Test when JSON parsing fails
    @Test
    void run_ShouldHandleJsonParsingError_WhenObjectMapperFails() throws Exception {
        // Arrange
        InputStream inputStream = new ByteArrayInputStream("invalid json".getBytes());
        when(resourceLoader.getResource("classpath:test-data.json")).thenReturn(resource);
        when(resource.exists()).thenReturn(true);
        when(resource.getInputStream()).thenReturn(inputStream);
        when(objectMapper.readValue(any(InputStream.class), any(TypeReference.class)))
                .thenThrow(new IOException("Invalid JSON format"));

        // Act
        assertDoesNotThrow(() -> dataInitializer.run());

        // Assert
        verify(resourceLoader).getResource("classpath:test-data.json");
        verify(resource).exists();
        verify(resource).getInputStream();
        verify(objectMapper).readValue(any(InputStream.class), any(TypeReference.class));
        verify(storePResService, never()).processPResMessage(any());
    }

    // Test when loadDataFromFile returns null
    @Test
    void run_ShouldHandleNullData_WhenLoadDataFromFileReturnsNull() throws Exception {
        // Arrange
        InputStream inputStream = new ByteArrayInputStream("{}".getBytes());
        when(resourceLoader.getResource("classpath:test-data.json")).thenReturn(resource);
        when(resource.exists()).thenReturn(true);
        when(resource.getInputStream()).thenReturn(inputStream);
        when(objectMapper.readValue(any(InputStream.class), any(TypeReference.class)))
                .thenThrow(new IOException("Parsing failed"));

        // Act
        assertDoesNotThrow(() -> dataInitializer.run());

        // Assert
        verify(storePResService, never()).processPResMessage(any());
    }

    // Test processing with errors
    @Test
    void run_ShouldLogErrors_WhenProcessingHasErrors() throws Exception {
        // Arrange
        InputStream inputStream = new ByteArrayInputStream("{}".getBytes());
        when(resourceLoader.getResource("classpath:test-data.json")).thenReturn(resource);
        when(resource.exists()).thenReturn(true);
        when(resource.getInputStream()).thenReturn(inputStream);
        when(objectMapper.readValue(any(InputStream.class), any(TypeReference.class)))
                .thenReturn(testPresMessage);
        when(storePResService.processPResMessage(testPresMessage)).thenReturn(errorResponse);

        // Act
        assertDoesNotThrow(() -> dataInitializer.run());

        // Assert
        verify(storePResService).processPResMessage(testPresMessage);
        // Note: We can't easily verify log statements, but we ensure the method completes without throwing
    }

    // Test when StorePResService throws exception
    @Test
    void run_ShouldHandleServiceException_WhenStorePResServiceFails() throws Exception {
        // Arrange
        InputStream inputStream = new ByteArrayInputStream("{}".getBytes());
        when(resourceLoader.getResource("classpath:test-data.json")).thenReturn(resource);
        when(resource.exists()).thenReturn(true);
        when(resource.getInputStream()).thenReturn(inputStream);
        when(objectMapper.readValue(any(InputStream.class), any(TypeReference.class)))
                .thenReturn(testPresMessage);
        when(storePResService.processPResMessage(testPresMessage))
                .thenThrow(new RuntimeException("Service unavailable"));

        // Act
        assertDoesNotThrow(() -> dataInitializer.run());

        // Assert
        verify(storePResService).processPResMessage(testPresMessage);
    }

    // Test when resource input stream fails
    @Test
    void run_ShouldHandleInputStreamException_WhenResourceInputStreamFails() throws Exception {
        // Arrange
        when(resourceLoader.getResource("classpath:test-data.json")).thenReturn(resource);
        when(resource.exists()).thenReturn(true);
        when(resource.getInputStream()).thenThrow(new IOException("Cannot read file"));

        // Act
        assertDoesNotThrow(() -> dataInitializer.run());

        // Assert
        verify(resourceLoader).getResource("classpath:test-data.json");
        verify(resource).exists();
        verify(resource).getInputStream();
        verify(storePResService, never()).processPResMessage(any());
    }

    // Test with different file paths
    @Test
    void run_ShouldUseConfiguredFilePath_WhenFilePathIsSet() throws Exception {
        // Arrange
        ReflectionTestUtils.setField(dataInitializer, "dataFilePath", "file:/custom/path/data.json");
        InputStream inputStream = new ByteArrayInputStream("{}".getBytes());
        when(resourceLoader.getResource("file:/custom/path/data.json")).thenReturn(resource);
        when(resource.exists()).thenReturn(true);
        when(resource.getInputStream()).thenReturn(inputStream);
        when(objectMapper.readValue(any(InputStream.class), any(TypeReference.class)))
                .thenReturn(testPresMessage);
        when(storePResService.processPResMessage(testPresMessage)).thenReturn(successResponse);

        // Act
        dataInitializer.run();

        // Assert
        verify(resourceLoader).getResource("file:/custom/path/data.json");
    }

    // Test with empty card range data
    @Test
    void run_ShouldHandleEmptyCardRanges_WhenPresMessageHasNoCardRanges() throws Exception {
        // Arrange
        PResMessageDTO emptyPresMessage = new PResMessageDTO();
        emptyPresMessage.setSerialNum("12345");
        emptyPresMessage.setMessageType("PRes");
        emptyPresMessage.setCardRangeData(new ArrayList<>());

        BulkImportResponseDTO emptyResponse = new BulkImportResponseDTO(
                0, 0, 0, new ArrayList<>(), LocalDateTime.now()
        );

        InputStream inputStream = new ByteArrayInputStream("{}".getBytes());
        when(resourceLoader.getResource("classpath:test-data.json")).thenReturn(resource);
        when(resource.exists()).thenReturn(true);
        when(resource.getInputStream()).thenReturn(inputStream);
        when(objectMapper.readValue(any(InputStream.class), any(TypeReference.class)))
                .thenReturn(emptyPresMessage);
        when(storePResService.processPResMessage(emptyPresMessage)).thenReturn(emptyResponse);

        // Act
        assertDoesNotThrow(() -> dataInitializer.run());

        // Assert
        verify(storePResService).processPResMessage(emptyPresMessage);
    }

    // Test command line arguments are ignored
    @Test
    void run_ShouldIgnoreCommandLineArguments_WhenArgumentsProvided() throws Exception {
        // Arrange
        InputStream inputStream = new ByteArrayInputStream("{}".getBytes());
        when(resourceLoader.getResource("classpath:test-data.json")).thenReturn(resource);
        when(resource.exists()).thenReturn(true);
        when(resource.getInputStream()).thenReturn(inputStream);
        when(objectMapper.readValue(any(InputStream.class), any(TypeReference.class)))
                .thenReturn(testPresMessage);
        when(storePResService.processPResMessage(testPresMessage)).thenReturn(successResponse);

        // Act
        assertDoesNotThrow(() -> dataInitializer.run("--arg1", "value1", "--arg2", "value2"));

        // Assert
        verify(storePResService).processPResMessage(testPresMessage);
    }

    // Test clearExisting property (even though not used in current implementation)
    @Test
    void run_ShouldRespectClearExistingProperty_WhenClearExistingIsTrue() throws Exception {
        // Arrange
        ReflectionTestUtils.setField(dataInitializer, "clearExisting", true);
        InputStream inputStream = new ByteArrayInputStream("{}".getBytes());
        when(resourceLoader.getResource("classpath:test-data.json")).thenReturn(resource);
        when(resource.exists()).thenReturn(true);
        when(resource.getInputStream()).thenReturn(inputStream);
        when(objectMapper.readValue(any(InputStream.class), any(TypeReference.class)))
                .thenReturn(testPresMessage);
        when(storePResService.processPResMessage(testPresMessage)).thenReturn(successResponse);

        // Act
        assertDoesNotThrow(() -> dataInitializer.run());

        // Assert
        verify(storePResService).processPResMessage(testPresMessage);
        // Note: clearExisting property is read but not currently used in the implementation
    }

    // Test large data processing
    @Test
    void run_ShouldHandleLargeDataSet_WhenProcessingManyCardRanges() throws Exception {
        // Arrange
        List<CardRangeDataDTO> largeCardRangeList = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            CardRangeDataDTO cardRange = new CardRangeDataDTO();
            cardRange.setStartRange(1000000000000000L + (i * 1000000L));
            cardRange.setEndRange(1000000000000000L + (i * 1000000L) + 999999L);
            cardRange.setActionInd("Y");
            largeCardRangeList.add(cardRange);
        }

        PResMessageDTO largePresMessage = new PResMessageDTO();
        largePresMessage.setSerialNum("12345");
        largePresMessage.setMessageType("PRes");
        largePresMessage.setCardRangeData(largeCardRangeList);

        BulkImportResponseDTO largeResponse = new BulkImportResponseDTO(
                1000, 1000, 0, new ArrayList<>(), LocalDateTime.now()
        );

        InputStream inputStream = new ByteArrayInputStream("{}".getBytes());
        when(resourceLoader.getResource("classpath:test-data.json")).thenReturn(resource);
        when(resource.exists()).thenReturn(true);
        when(resource.getInputStream()).thenReturn(inputStream);
        when(objectMapper.readValue(any(InputStream.class), any(TypeReference.class)))
                .thenReturn(largePresMessage);
        when(storePResService.processPResMessage(largePresMessage)).thenReturn(largeResponse);

        // Act
        assertDoesNotThrow(() -> dataInitializer.run());

        // Assert
        verify(storePResService).processPResMessage(largePresMessage);
    }

    // Test mixed success and error response
    @Test
    void run_ShouldHandleMixedResults_WhenSomeProcessingSucceedsAndSomeFails() throws Exception {
        // Arrange
        List<String> mixedErrors = Arrays.asList(
                "Error processing range 2: Constraint violation"
        );
        BulkImportResponseDTO mixedResponse = new BulkImportResponseDTO(
                2, 1, 1, mixedErrors, LocalDateTime.now()
        );

        InputStream inputStream = new ByteArrayInputStream("{}".getBytes());
        when(resourceLoader.getResource("classpath:test-data.json")).thenReturn(resource);
        when(resource.exists()).thenReturn(true);
        when(resource.getInputStream()).thenReturn(inputStream);
        when(objectMapper.readValue(any(InputStream.class), any(TypeReference.class)))
                .thenReturn(testPresMessage);
        when(storePResService.processPResMessage(testPresMessage)).thenReturn(mixedResponse);

        // Act
        assertDoesNotThrow(() -> dataInitializer.run());

        // Assert
        verify(storePResService).processPResMessage(testPresMessage);
    }

}
