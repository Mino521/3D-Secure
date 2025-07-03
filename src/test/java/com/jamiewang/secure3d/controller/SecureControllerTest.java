package com.jamiewang.secure3d.controller;

import com.jamiewang.secure3d.dto.CardRangeDataDTO;
import com.jamiewang.secure3d.service.ILookUpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SecureControllerTest {

    @Mock
    private ILookUpService lookUpService;

    @InjectMocks
    private SecureController secureController;
    private CardRangeDataDTO testCardRangeData;

    @BeforeEach
    void setUp() {

        // Setup test card range data
        testCardRangeData = new CardRangeDataDTO();
        testCardRangeData.setStartRange(1234567890000000L);
        testCardRangeData.setEndRange(1234567890999999L);
        testCardRangeData.setActionInd("Y");
        testCardRangeData.setThreeDsMethodUrl("https://example.com/3ds-method");
        testCardRangeData.setAcsStartProtocolVersion("2.1.0");
        testCardRangeData.setAcsEndProtocolVersion("2.2.0");
        testCardRangeData.setAcsInfoInd(Arrays.asList("01", "02", "03"));
    }

    // ==================== Direct Method Tests ====================

    @Test
    void lookupByPan_ShouldReturnCardRangeData_WhenPanIsFound() {
        // Arrange
        Long testPan = 1234567890123456L;
        when(lookUpService.lookupByPan(testPan)).thenReturn(Optional.of(testCardRangeData));

        // Act
        ResponseEntity<CardRangeDataDTO> response = secureController.lookupByPan(testPan);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(testCardRangeData, response.getBody());
        assertEquals(testCardRangeData.getStartRange(), response.getBody().getStartRange());
        assertEquals(testCardRangeData.getEndRange(), response.getBody().getEndRange());
        assertEquals(testCardRangeData.getActionInd(), response.getBody().getActionInd());
        assertEquals(testCardRangeData.getThreeDsMethodUrl(), response.getBody().getThreeDsMethodUrl());

        verify(lookUpService).lookupByPan(testPan);
    }

    @Test
    void lookupByPan_ShouldReturnNotFound_WhenPanIsNotFound() {
        // Arrange
        Long testPan = 9999999999999999L;
        when(lookUpService.lookupByPan(testPan)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<CardRangeDataDTO> response = secureController.lookupByPan(testPan);

        // Assert
        assertNotNull(response);
        assertEquals(404, response.getStatusCodeValue());
        assertNull(response.getBody());

        verify(lookUpService).lookupByPan(testPan);
    }

    @Test
    void lookupByPan_ShouldHandleNullPan() {
        // Arrange
        Long nullPan = null;
        when(lookUpService.lookupByPan(nullPan)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<CardRangeDataDTO> response = secureController.lookupByPan(nullPan);

        // Assert
        assertNotNull(response);
        assertEquals(404, response.getStatusCodeValue());
        assertNull(response.getBody());

        verify(lookUpService).lookupByPan(nullPan);
    }

    // ==================== Service Exception Handling Tests ====================

    @Test
    void lookupByPan_ShouldPropagateException_WhenServiceThrowsException() {
        // Arrange
        Long testPan = 1234567890123456L;
        when(lookUpService.lookupByPan(testPan)).thenThrow(new RuntimeException("Database connection failed"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> secureController.lookupByPan(testPan));

        verify(lookUpService).lookupByPan(testPan);
    }

    // ==================== Performance and Edge Case Tests ====================

    @Test
    void lookupByPan_ShouldHandleMultipleConsecutiveCalls() {
        // Arrange
        Long testPan1 = 1234567890123456L;
        Long testPan2 = 5555555555555555L;
        when(lookUpService.lookupByPan(testPan1)).thenReturn(Optional.of(testCardRangeData));
        when(lookUpService.lookupByPan(testPan2)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<CardRangeDataDTO> response1 = secureController.lookupByPan(testPan1);
        ResponseEntity<CardRangeDataDTO> response2 = secureController.lookupByPan(testPan2);

        // Assert
        assertEquals(200, response1.getStatusCodeValue());
        assertNotNull(response1.getBody());
        assertEquals(404, response2.getStatusCodeValue());
        assertNull(response2.getBody());

        verify(lookUpService).lookupByPan(testPan1);
        verify(lookUpService).lookupByPan(testPan2);
    }

}
