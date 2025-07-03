package com.jamiewang.secure3d.service.impl;

import com.jamiewang.secure3d.dto.BulkImportResponseDTO;
import com.jamiewang.secure3d.dto.CardRangeDataDTO;
import com.jamiewang.secure3d.dto.PResMessageDTO;
import com.jamiewang.secure3d.entity.CardRangeEntity;
import com.jamiewang.secure3d.repository.ICardRangeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StorePResServiceImplTest {

    @Mock
    private ICardRangeRepository cardRangeRepository;

    @InjectMocks
    private StorePResServiceImpl storePResService;

    private PResMessageDTO testPresMessage;
    private CardRangeDataDTO testCardRangeData1;
    private CardRangeDataDTO testCardRangeData2;

    @BeforeEach
    void setUp() {
        // Setup test card range data 1
        testCardRangeData1 = new CardRangeDataDTO();
        testCardRangeData1.setStartRange(1234567890000000L);
        testCardRangeData1.setEndRange(1234567890999999L);
        testCardRangeData1.setActionInd("Y");
        testCardRangeData1.setThreeDsMethodUrl("https://example.com/3ds1");
        testCardRangeData1.setAcsStartProtocolVersion("2.1.0");
        testCardRangeData1.setAcsEndProtocolVersion("2.2.0");
        testCardRangeData1.setAcsInfoInd(Arrays.asList("01", "02", "03"));

        // Setup test card range data 2
        testCardRangeData2 = new CardRangeDataDTO();
        testCardRangeData2.setStartRange(5555555555000000L);
        testCardRangeData2.setEndRange(5555555555999999L);
        testCardRangeData2.setActionInd("N");
        testCardRangeData2.setThreeDsMethodUrl("https://example.com/3ds2");
        testCardRangeData2.setAcsStartProtocolVersion("2.1.0");
        testCardRangeData2.setAcsEndProtocolVersion("2.2.0");
        testCardRangeData2.setAcsInfoInd(Arrays.asList("01", "02"));

        // Setup test PRes message
        testPresMessage = new PResMessageDTO();
        testPresMessage.setSerialNum("12345");
        testPresMessage.setMessageType("PRes");
        testPresMessage.setCardRangeData(Arrays.asList(testCardRangeData1, testCardRangeData2));
    }

    // Test successful processing of all card ranges
    @Test
    void processPResMessage_ShouldProcessAllRangesSuccessfully_WhenAllDataIsValid() {
        // Arrange
        when(cardRangeRepository.save(any(CardRangeEntity.class)))
                .thenReturn(new CardRangeEntity());

        // Act
        BulkImportResponseDTO result = storePResService.processPResMessage(testPresMessage);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getTotalProcessed());
        assertEquals(2, result.getSuccessCount());
        assertEquals(0, result.getErrorCount());
        assertTrue(result.getErrors().isEmpty());
        assertNotNull(result.getProcessedAt());

        // Verify repository interactions
        verify(cardRangeRepository, times(2)).save(any(CardRangeEntity.class));

        // Capture and verify the saved entities
        ArgumentCaptor<CardRangeEntity> entityCaptor = ArgumentCaptor.forClass(CardRangeEntity.class);
        verify(cardRangeRepository, times(2)).save(entityCaptor.capture());

        List<CardRangeEntity> savedEntities = entityCaptor.getAllValues();
        assertEquals(2, savedEntities.size());

        // Verify first entity
        CardRangeEntity entity1 = savedEntities.get(0);
        assertEquals(testCardRangeData1.getStartRange(), entity1.getStartRange());
        assertEquals(testCardRangeData1.getEndRange(), entity1.getEndRange());
        assertEquals(testCardRangeData1.getActionInd(), entity1.getActionInd());
        assertEquals(testCardRangeData1.getThreeDsMethodUrl(), entity1.getThreeDsMethodUrl());
        assertEquals(testCardRangeData1.getAcsStartProtocolVersion(), entity1.getAcsStartProtocolVersion());
        assertEquals(testCardRangeData1.getAcsEndProtocolVersion(), entity1.getAcsEndProtocolVersion());
        assertEquals(testCardRangeData1.getAcsInfoInd(), entity1.getAcsInfoInd());
        assertNotNull(entity1.getCreatedAt());
        assertNotNull(entity1.getUpdatedAt());

        // Verify second entity
        CardRangeEntity entity2 = savedEntities.get(1);
        assertEquals(testCardRangeData2.getStartRange(), entity2.getStartRange());
        assertEquals(testCardRangeData2.getEndRange(), entity2.getEndRange());
        assertEquals(testCardRangeData2.getActionInd(), entity2.getActionInd());
    }

    // Test partial failure scenario
    @Test
    void processPResMessage_ShouldHandlePartialFailure_WhenSomeRangesFail() {
        // Arrange
        when(cardRangeRepository.save(any(CardRangeEntity.class)))
                .thenReturn(new CardRangeEntity()) // First save succeeds
                .thenThrow(new RuntimeException("Database constraint violation")); // Second save fails

        // Act
        BulkImportResponseDTO result = storePResService.processPResMessage(testPresMessage);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getTotalProcessed());
        assertEquals(1, result.getSuccessCount());
        assertEquals(1, result.getErrorCount());
        assertEquals(1, result.getErrors().size());

        String errorMessage = result.getErrors().get(0);
        assertTrue(errorMessage.contains("Error processing range 2"));
        assertTrue(errorMessage.contains("5555555555000000-5555555555999999"));
        assertTrue(errorMessage.contains("Database constraint violation"));

        // Verify repository was called twice
        verify(cardRangeRepository, times(2)).save(any(CardRangeEntity.class));
    }

    // Test complete failure scenario
    @Test
    void processPResMessage_ShouldHandleCompleteFailure_WhenAllRangesFail() {
        // Arrange
        when(cardRangeRepository.save(any(CardRangeEntity.class)))
                .thenThrow(new RuntimeException("Database connection failed"));

        // Act
        BulkImportResponseDTO result = storePResService.processPResMessage(testPresMessage);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getTotalProcessed());
        assertEquals(0, result.getSuccessCount());
        assertEquals(2, result.getErrorCount());
        assertEquals(2, result.getErrors().size());

        // Verify both errors are recorded
        assertTrue(result.getErrors().get(0).contains("Error processing range 1"));
        assertTrue(result.getErrors().get(1).contains("Error processing range 2"));
        assertTrue(result.getErrors().stream().allMatch(error -> error.contains("Database connection failed")));

        verify(cardRangeRepository, times(2)).save(any(CardRangeEntity.class));
    }

    // Test empty card range list
    @Test
    void processPResMessage_ShouldHandleEmptyCardRangeList() {
        // Arrange
        PResMessageDTO emptyMessage = new PResMessageDTO();
        emptyMessage.setSerialNum("12345");
        emptyMessage.setMessageType("PRes");
        emptyMessage.setCardRangeData(new ArrayList<>());

        // Act
        BulkImportResponseDTO result = storePResService.processPResMessage(emptyMessage);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getTotalProcessed());
        assertEquals(0, result.getSuccessCount());
        assertEquals(0, result.getErrorCount());
        assertTrue(result.getErrors().isEmpty());

        // Verify repository was never called
        verify(cardRangeRepository, never()).save(any(CardRangeEntity.class));
    }

    // Test single card range
    @Test
    void processPResMessage_ShouldProcessSingleRange_WhenOnlyOneRangeProvided() {
        // Arrange
        PResMessageDTO singleRangeMessage = new PResMessageDTO();
        singleRangeMessage.setSerialNum("12345");
        singleRangeMessage.setMessageType("PRes");
        singleRangeMessage.setCardRangeData(Arrays.asList(testCardRangeData1));

        when(cardRangeRepository.save(any(CardRangeEntity.class)))
                .thenReturn(new CardRangeEntity());

        // Act
        BulkImportResponseDTO result = storePResService.processPResMessage(singleRangeMessage);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalProcessed());
        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getErrorCount());
        assertTrue(result.getErrors().isEmpty());

        verify(cardRangeRepository, times(1)).save(any(CardRangeEntity.class));
    }

    // Test entity creation with null values
    @Test
    void processPResMessage_ShouldHandleNullValues_InCardRangeData() {
        // Arrange
        CardRangeDataDTO cardRangeWithNulls = new CardRangeDataDTO();
        cardRangeWithNulls.setStartRange(1111111111000000L);
        cardRangeWithNulls.setEndRange(1111111111999999L);
        cardRangeWithNulls.setActionInd(null);
        cardRangeWithNulls.setThreeDsMethodUrl(null);
        cardRangeWithNulls.setAcsStartProtocolVersion(null);
        cardRangeWithNulls.setAcsEndProtocolVersion(null);
        cardRangeWithNulls.setAcsInfoInd(null);

        PResMessageDTO messageWithNulls = new PResMessageDTO();
        messageWithNulls.setSerialNum("12345");
        messageWithNulls.setMessageType("PRes");
        messageWithNulls.setCardRangeData(Arrays.asList(cardRangeWithNulls));

        when(cardRangeRepository.save(any(CardRangeEntity.class)))
                .thenReturn(new CardRangeEntity());

        // Act
        BulkImportResponseDTO result = storePResService.processPResMessage(messageWithNulls);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalProcessed());
        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getErrorCount());
        assertTrue(result.getErrors().isEmpty());

        // Verify the entity was created with null values
        ArgumentCaptor<CardRangeEntity> entityCaptor = ArgumentCaptor.forClass(CardRangeEntity.class);
        verify(cardRangeRepository).save(entityCaptor.capture());

        CardRangeEntity savedEntity = entityCaptor.getValue();
        assertEquals(cardRangeWithNulls.getStartRange(), savedEntity.getStartRange());
        assertEquals(cardRangeWithNulls.getEndRange(), savedEntity.getEndRange());
        assertNull(savedEntity.getActionInd());
        assertNull(savedEntity.getThreeDsMethodUrl());
        assertNull(savedEntity.getAcsStartProtocolVersion());
        assertNull(savedEntity.getAcsEndProtocolVersion());
        assertNull(savedEntity.getAcsInfoInd());
        assertNotNull(savedEntity.getCreatedAt());
        assertNotNull(savedEntity.getUpdatedAt());
    }

    // Test error message formatting
    @Test
    void processPResMessage_ShouldFormatErrorMessagesCorrectly() {
        // Arrange
        when(cardRangeRepository.save(any(CardRangeEntity.class)))
                .thenThrow(new IllegalArgumentException("Invalid range values"));

        // Act
        BulkImportResponseDTO result = storePResService.processPResMessage(testPresMessage);

        // Assert
        assertEquals(2, result.getErrors().size());

        String firstError = result.getErrors().get(0);
        assertTrue(firstError.contains("Error processing range 1"));
        assertTrue(firstError.contains("1234567890000000-1234567890999999"));
        assertTrue(firstError.contains("Invalid range values"));

        String secondError = result.getErrors().get(1);
        assertTrue(secondError.contains("Error processing range 2"));
        assertTrue(secondError.contains("5555555555000000-5555555555999999"));
        assertTrue(secondError.contains("Invalid range values"));
    }

    // Test large number of card ranges
    @Test
    void processPResMessage_ShouldHandleLargeNumberOfRanges() {
        // Arrange
        List<CardRangeDataDTO> largeCardRangeList = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            CardRangeDataDTO cardRange = new CardRangeDataDTO();
            cardRange.setStartRange(1000000000000000L + (i * 1000000L));
            cardRange.setEndRange(1000000000000000L + (i * 1000000L) + 999999L);
            cardRange.setActionInd("Y");
            largeCardRangeList.add(cardRange);
        }

        PResMessageDTO largeMessage = new PResMessageDTO();
        largeMessage.setSerialNum("12345");
        largeMessage.setMessageType("PRes");
        largeMessage.setCardRangeData(largeCardRangeList);

        when(cardRangeRepository.save(any(CardRangeEntity.class)))
                .thenReturn(new CardRangeEntity());

        // Act
        BulkImportResponseDTO result = storePResService.processPResMessage(largeMessage);

        // Assert
        assertNotNull(result);
        assertEquals(100, result.getTotalProcessed());
        assertEquals(100, result.getSuccessCount());
        assertEquals(0, result.getErrorCount());
        assertTrue(result.getErrors().isEmpty());

        verify(cardRangeRepository, times(100)).save(any(CardRangeEntity.class));
    }

    // Test mixed success and failure scenario with specific indices
    @Test
    void processPResMessage_ShouldHandleMixedResults_WithCorrectIndices() {
        // Arrange - Add a third card range for more complex testing
        CardRangeDataDTO testCardRangeData3 = new CardRangeDataDTO();
        testCardRangeData3.setStartRange(9999999999000000L);
        testCardRangeData3.setEndRange(9999999999999999L);
        testCardRangeData3.setActionInd("Y");

        PResMessageDTO threeRangeMessage = new PResMessageDTO();
        threeRangeMessage.setSerialNum("12345");
        threeRangeMessage.setMessageType("PRes");
        threeRangeMessage.setCardRangeData(Arrays.asList(testCardRangeData1, testCardRangeData2, testCardRangeData3));

        // First save succeeds, second fails, third succeeds
        when(cardRangeRepository.save(any(CardRangeEntity.class)))
                .thenReturn(new CardRangeEntity())
                .thenThrow(new RuntimeException("Constraint violation"))
                .thenReturn(new CardRangeEntity());

        // Act
        BulkImportResponseDTO result = storePResService.processPResMessage(threeRangeMessage);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.getTotalProcessed());
        assertEquals(2, result.getSuccessCount());
        assertEquals(1, result.getErrorCount());
        assertEquals(1, result.getErrors().size());

        String errorMessage = result.getErrors().get(0);
        assertTrue(errorMessage.contains("Error processing range 2"));
        assertTrue(errorMessage.contains("5555555555000000-5555555555999999"));

        verify(cardRangeRepository, times(3)).save(any(CardRangeEntity.class));
    }

    // Test timestamp creation
    @Test
    void processPResMessage_ShouldSetTimestamps_OnCreatedEntities() {
        // Arrange
        LocalDateTime beforeTest = LocalDateTime.now().minusSeconds(1);
        when(cardRangeRepository.save(any(CardRangeEntity.class)))
                .thenReturn(new CardRangeEntity());

        // Act
        BulkImportResponseDTO result = storePResService.processPResMessage(testPresMessage);
        LocalDateTime afterTest = LocalDateTime.now().plusSeconds(1);

        // Assert
        ArgumentCaptor<CardRangeEntity> entityCaptor = ArgumentCaptor.forClass(CardRangeEntity.class);
        verify(cardRangeRepository, times(2)).save(entityCaptor.capture());

        List<CardRangeEntity> savedEntities = entityCaptor.getAllValues();
        for (CardRangeEntity entity : savedEntities) {
            assertNotNull(entity.getCreatedAt());
            assertNotNull(entity.getUpdatedAt());
            assertTrue(entity.getCreatedAt().isAfter(beforeTest));
            assertTrue(entity.getCreatedAt().isBefore(afterTest));
            assertTrue(entity.getUpdatedAt().isAfter(beforeTest));
            assertTrue(entity.getUpdatedAt().isBefore(afterTest));
        }

        // Verify response timestamp
        assertNotNull(result.getProcessedAt());
        assertTrue(result.getProcessedAt().isAfter(beforeTest));
        assertTrue(result.getProcessedAt().isBefore(afterTest));
    }

}
