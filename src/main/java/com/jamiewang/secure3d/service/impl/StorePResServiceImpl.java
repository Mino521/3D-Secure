package com.jamiewang.secure3d.service.impl;

import com.jamiewang.secure3d.dto.BulkImportResponseDTO;
import com.jamiewang.secure3d.dto.CardRangeDataDTO;
import com.jamiewang.secure3d.dto.PResMessageDTO;
import com.jamiewang.secure3d.entity.CardRangeEntity;
import com.jamiewang.secure3d.repository.ICardRangeRepository;
import com.jamiewang.secure3d.service.IStorePResService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class StorePResServiceImpl implements IStorePResService {

    @Autowired
    private ICardRangeRepository cardRangeRepository;

    /**
     * Process a PRes message containing multiple card ranges
     *
     * @param presMessage Complete PRes message with card range data
     * @return Bulk import response with processing statistics
     */
    @Transactional
    public BulkImportResponseDTO processPResMessage(PResMessageDTO presMessage) {
        log.info("Processing PRes message: serialNum={}, messageType={}, cardRanges={}",
                presMessage.getSerialNum(), presMessage.getMessageType(), presMessage.getCardRangeData().size());

        List<String> errors = new ArrayList<>();
        int successCount = 0;
        int errorCount = 0;
        int totalProcessed = presMessage.getCardRangeData().size();

        for (int i = 0; i < presMessage.getCardRangeData().size(); i++) {
            CardRangeDataDTO cardRangeData = presMessage.getCardRangeData().get(i);

            try {
                log.debug("Processing card range data: {} - {}",
                        cardRangeData.getStartRange(), cardRangeData.getEndRange());

                // TODO： Consider delete and update action. When delete and update card range data, remember to remove
                //  influenced cached data (recording to cached_record table).

                // Here we only consider adding card ranges
                CardRangeEntity cardRange = createCardRangeFromData(cardRangeData);
                log.debug("Creating new card range: {} - {}", cardRangeData.getStartRange(), cardRangeData.getEndRange());

                cardRangeRepository.save(cardRange);

                successCount++;
            } catch (Exception e) {
                errorCount++;
                String errorMsg = String.format("Error processing range %d (%s-%s): %s",
                        i + 1, cardRangeData.getStartRange(), cardRangeData.getEndRange(), e.getMessage());
                errors.add(errorMsg);
                log.warn(errorMsg);
            }
        }

        log.info("PRes message processing completed: {} successful, {} errors out of {} total",
                successCount, errorCount, totalProcessed);

        return new BulkImportResponseDTO(totalProcessed, successCount, errorCount, errors, LocalDateTime.now());
    }

    /**
     * Create CardRange entity from CardRangeDataDTO
     */
    private CardRangeEntity createCardRangeFromData(CardRangeDataDTO data) {
        CardRangeEntity cardRangeEntity = new CardRangeEntity();
        cardRangeEntity.setStartRange(data.getStartRange());
        cardRangeEntity.setEndRange(data.getEndRange());
        cardRangeEntity.setThreeDsMethodUrl(data.getThreeDsMethodUrl());
        cardRangeEntity.setAcsInfoInd(data.getAcsInfoInd());
        cardRangeEntity.setActionInd(data.getActionInd());
        cardRangeEntity.setAcsStartProtocolVersion(data.getAcsStartProtocolVersion());
        cardRangeEntity.setAcsEndProtocolVersion(data.getAcsEndProtocolVersion());
        cardRangeEntity.setCreatedAt(LocalDateTime.now());
        cardRangeEntity.setUpdatedAt(LocalDateTime.now());

        return cardRangeEntity;
    }
}
