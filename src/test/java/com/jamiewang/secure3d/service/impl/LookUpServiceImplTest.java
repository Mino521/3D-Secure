package com.jamiewang.secure3d.service.impl;

import com.jamiewang.secure3d.dto.CardRangeDataDTO;
import com.jamiewang.secure3d.entity.CardRangeEntity;
import com.jamiewang.secure3d.repository.ICardRangeRepository;
import com.jamiewang.secure3d.service.IRedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LookUpServiceImplTest {

    @Mock
    private ICardRangeRepository cardRangeRepository;

    @Mock
    private IRedisService redisService;

    @InjectMocks
    private LookUpServiceImpl lookUpService;

    private static final Long TEST_PAN = 1234567890123456L;
    private static final String REDIS_KEY = "look_up_1234567890123456";

    private CardRangeEntity testCardRangeEntity;
    private CardRangeDataDTO testCardRangeDTO;

    @BeforeEach
    void setUp() {
        // Setup test entities
        testCardRangeEntity = new CardRangeEntity();
        testCardRangeEntity.setStartRange(1234567890000000L);
        testCardRangeEntity.setEndRange(1234567890999999L);
        testCardRangeEntity.setActionInd("Y");
        testCardRangeEntity.setThreeDsMethodUrl("https://example.com/3ds");
        testCardRangeEntity.setAcsEndProtocolVersion("2.2.0");
        testCardRangeEntity.setAcsStartProtocolVersion("2.1.0");
        testCardRangeEntity.setAcsInfoInd("01,02,03");

        testCardRangeDTO = new CardRangeDataDTO();
        testCardRangeDTO.setStartRange(1234567890000000L);
        testCardRangeDTO.setEndRange(1234567890999999L);
        testCardRangeDTO.setActionInd("Y");
        testCardRangeDTO.setThreeDsMethodUrl("https://example.com/3ds");
        testCardRangeDTO.setAcsEndProtocolVersion("2.2.0");
        testCardRangeDTO.setAcsStartProtocolVersion("2.1.0");
        testCardRangeDTO.setAcsInfoInd("01,02,03");
    }

    // Test cases for successful cache hit
    @Test
    void lookupByPan_ShouldReturnFromCache_WhenDataExistsInRedis() {
        // Arrange
        when(redisService.findOne(REDIS_KEY, CardRangeDataDTO.class))
                .thenReturn(Optional.of(testCardRangeDTO));

        // Act
        Optional<CardRangeDataDTO> result = lookUpService.lookupByPan(TEST_PAN);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testCardRangeDTO, result.get());

        // Verify cache was checked first
        verify(redisService).findOne(REDIS_KEY, CardRangeDataDTO.class);

        // Verify database was not accessed since cache hit
        verify(cardRangeRepository, never()).findByPanInRange(any());
        verify(redisService, never()).writeOne(anyString(), any());
    }

    // Test cases for cache miss but database hit
    @Test
    void lookupByPan_ShouldReturnFromDatabase_WhenNotInCacheButInDatabase() {
        // Arrange
        when(redisService.findOne(REDIS_KEY, CardRangeDataDTO.class))
                .thenReturn(Optional.empty());
        when(cardRangeRepository.findByPanInRange(TEST_PAN))
                .thenReturn(Optional.of(testCardRangeEntity));
        when(redisService.writeOne(eq(REDIS_KEY), any(CardRangeDataDTO.class)))
                .thenReturn(true);

        // Act
        Optional<CardRangeDataDTO> result = lookUpService.lookupByPan(TEST_PAN);

        // Assert
        assertTrue(result.isPresent());
        CardRangeDataDTO dto = result.get();

        // Verify DTO mapping
        assertEquals(testCardRangeEntity.getStartRange(), dto.getStartRange());
        assertEquals(testCardRangeEntity.getEndRange(), dto.getEndRange());
        assertEquals(testCardRangeEntity.getActionInd(), dto.getActionInd());
        assertEquals(testCardRangeEntity.getThreeDsMethodUrl(), dto.getThreeDsMethodUrl());
        assertEquals(testCardRangeEntity.getAcsEndProtocolVersion(), dto.getAcsEndProtocolVersion());
        assertEquals(testCardRangeEntity.getAcsStartProtocolVersion(), dto.getAcsStartProtocolVersion());
        assertEquals(testCardRangeEntity.getAcsInfoInd(), dto.getAcsInfoInd());

        // Verify the flow
        verify(redisService).findOne(REDIS_KEY, CardRangeDataDTO.class);
        verify(cardRangeRepository).findByPanInRange(TEST_PAN);
        verify(redisService).writeOne(eq(REDIS_KEY), any(CardRangeDataDTO.class));
    }

    // Test case for not found in cache or database
    @Test
    void lookupByPan_ShouldReturnEmpty_WhenNotFoundInCacheOrDatabase() {
        // Arrange
        when(redisService.findOne(REDIS_KEY, CardRangeDataDTO.class))
                .thenReturn(Optional.empty());
        when(cardRangeRepository.findByPanInRange(TEST_PAN))
                .thenReturn(Optional.empty());

        // Act
        Optional<CardRangeDataDTO> result = lookUpService.lookupByPan(TEST_PAN);

        // Assert
        assertFalse(result.isPresent());

        // Verify the flow
        verify(redisService).findOne(REDIS_KEY, CardRangeDataDTO.class);
        verify(cardRangeRepository).findByPanInRange(TEST_PAN);
        verify(redisService, never()).writeOne(anyString(), any());
    }

    // Test case for null PAN
    @Test
    void lookupByPan_ShouldReturnEmpty_WhenPanIsNull() {
        // Act
        Optional<CardRangeDataDTO> result = lookUpService.lookupByPan(null);

        // Assert
        assertFalse(result.isPresent());

        // Verify no services were called
        verify(redisService, never()).findOne(anyString(), any());
        verify(cardRangeRepository, never()).findByPanInRange(any());
        verify(redisService, never()).writeOne(anyString(), any());
    }

    // Test case for database failure after cache miss
    @Test
    void lookupByPan_ShouldReturnEmpty_WhenDatabaseFails() {
        // Arrange
        when(redisService.findOne(REDIS_KEY, CardRangeDataDTO.class))
                .thenReturn(Optional.empty());
        when(cardRangeRepository.findByPanInRange(TEST_PAN))
                .thenThrow(new RuntimeException("Database connection failed"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> lookUpService.lookupByPan(TEST_PAN));

        // Verify the flow
        verify(redisService).findOne(REDIS_KEY, CardRangeDataDTO.class);
        verify(cardRangeRepository).findByPanInRange(TEST_PAN);
        verify(redisService, never()).writeOne(anyString(), any());
    }

    // Test case for cache write failure (should still return result)
    @Test
    void lookupByPan_ShouldReturnResult_WhenCacheWriteFails() {
        // Arrange
        when(redisService.findOne(REDIS_KEY, CardRangeDataDTO.class))
                .thenReturn(Optional.empty());
        when(cardRangeRepository.findByPanInRange(TEST_PAN))
                .thenReturn(Optional.of(testCardRangeEntity));
        when(redisService.writeOne(eq(REDIS_KEY), any(CardRangeDataDTO.class)))
                .thenReturn(false); // Write fails

        // Act
        Optional<CardRangeDataDTO> result = lookUpService.lookupByPan(TEST_PAN);

        // Assert - Should still return the result even if cache write fails
        assertTrue(result.isPresent());
        assertEquals(testCardRangeEntity.getStartRange(), result.get().getStartRange());

        // Verify the flow
        verify(redisService).findOne(REDIS_KEY, CardRangeDataDTO.class);
        verify(cardRangeRepository).findByPanInRange(TEST_PAN);
        verify(redisService).writeOne(eq(REDIS_KEY), any(CardRangeDataDTO.class));
    }

    // Test case with edge case PAN values
    @Test
    void lookupByPan_ShouldHandleEdgeCasePan_WhenPanIsZero() {
        // Arrange
        Long zeroPan = 0L;
        String zeroRedisKey = "look_up_0";
        when(redisService.findOne(zeroRedisKey, CardRangeDataDTO.class))
                .thenReturn(Optional.empty());
        when(cardRangeRepository.findByPanInRange(zeroPan))
                .thenReturn(Optional.empty());

        // Act
        Optional<CardRangeDataDTO> result = lookUpService.lookupByPan(zeroPan);

        // Assert
        assertFalse(result.isPresent());
        verify(redisService).findOne(zeroRedisKey, CardRangeDataDTO.class);
        verify(cardRangeRepository).findByPanInRange(zeroPan);
    }

    @Test
    void lookupByPan_ShouldHandleMaxLongValue() {
        // Arrange
        Long maxPan = Long.MAX_VALUE;
        String maxRedisKey = "look_up_" + Long.MAX_VALUE;
        when(redisService.findOne(maxRedisKey, CardRangeDataDTO.class))
                .thenReturn(Optional.of(testCardRangeDTO));

        // Act
        Optional<CardRangeDataDTO> result = lookUpService.lookupByPan(maxPan);

        // Assert
        assertTrue(result.isPresent());
        verify(redisService).findOne(maxRedisKey, CardRangeDataDTO.class);
        verify(cardRangeRepository, never()).findByPanInRange(any());
    }

    // Test entity to DTO mapping with null values
    @Test
    void lookupByPan_ShouldHandleNullFieldsInEntity() {
        // Arrange
        CardRangeEntity entityWithNulls = new CardRangeEntity();
        entityWithNulls.setStartRange(1234567890000000L);
        entityWithNulls.setEndRange(1234567890999999L);
        entityWithNulls.setActionInd(null);
        entityWithNulls.setThreeDsMethodUrl(null);
        entityWithNulls.setAcsEndProtocolVersion(null);
        entityWithNulls.setAcsStartProtocolVersion(null);
        entityWithNulls.setAcsInfoInd(null);

        when(redisService.findOne(REDIS_KEY, CardRangeDataDTO.class))
                .thenReturn(Optional.empty());
        when(cardRangeRepository.findByPanInRange(TEST_PAN))
                .thenReturn(Optional.of(entityWithNulls));
        when(redisService.writeOne(eq(REDIS_KEY), any(CardRangeDataDTO.class)))
                .thenReturn(true);

        // Act
        Optional<CardRangeDataDTO> result = lookUpService.lookupByPan(TEST_PAN);

        // Assert
        assertTrue(result.isPresent());
        CardRangeDataDTO dto = result.get();
        assertEquals(entityWithNulls.getStartRange(), dto.getStartRange());
        assertEquals(entityWithNulls.getEndRange(), dto.getEndRange());
        assertNull(dto.getActionInd());
        assertNull(dto.getThreeDsMethodUrl());
        assertNull(dto.getAcsEndProtocolVersion());
        assertNull(dto.getAcsStartProtocolVersion());
        assertNull(dto.getAcsInfoInd());
    }

    // Test Redis key construction
    @Test
    void lookupByPan_ShouldConstructCorrectRedisKey() {
        // Arrange
        Long testPan = 9876543210987654L;
        String expectedKey = "look_up_9876543210987654";
        when(redisService.findOne(expectedKey, CardRangeDataDTO.class))
                .thenReturn(Optional.of(testCardRangeDTO));

        // Act
        lookUpService.lookupByPan(testPan);

        // Assert
        verify(redisService).findOne(expectedKey, CardRangeDataDTO.class);
    }

}
