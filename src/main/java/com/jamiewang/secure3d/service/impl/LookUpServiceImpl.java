package com.jamiewang.secure3d.service.impl;

import com.jamiewang.secure3d.dto.CardRangeDataDTO;
import com.jamiewang.secure3d.entity.CardRangeEntity;
import com.jamiewang.secure3d.repository.ICardRangeRepository;
import com.jamiewang.secure3d.service.ILookUpService;
import com.jamiewang.secure3d.service.IRedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Slf4j
public class LookUpServiceImpl implements ILookUpService {

    @Autowired
    private ICardRangeRepository cardRangeRepository;

    @Autowired
    private IRedisService redisService;

    private static final String LOOK_UP_FUNC_PREFIX = "look_up_";

    /**
     * Lookup card range by PAN
     *
     * @param pan Primary Account Number
     * @return Optional containing the lookup response, empty if not found
     */
    @Transactional
    public Optional<CardRangeDataDTO> lookupByPan(Long pan) {
        log.debug("Looking up card range for PAN: {}", pan);

        if (pan == null) {
            return Optional.empty();
        }

        // First try Redis cache
        Optional<CardRangeDataDTO> result = redisService.findOne(LOOK_UP_FUNC_PREFIX+String.valueOf(pan), CardRangeDataDTO.class);

        if (result.isPresent()) {
            log.debug("Found card range in shared tree");
            return result;
        }

        // Fallback to database if not found in cache
        log.debug("No match found in shared tree, falling back to database");

        Optional<CardRangeEntity> databaseResult = cardRangeRepository.findByPanInRange(pan);
        if (databaseResult.isPresent()) {
            log.debug("Found card range in database");
            CardRangeDataDTO dto = cardRangeEntityToDTO(databaseResult.get());

            // Write to cache
            redisService.writeOne(LOOK_UP_FUNC_PREFIX+String.valueOf(pan), dto);
            // TODO: Write a line of record to cached_record table.

            return Optional.of(dto);
        }

        return Optional.empty();
    }

    private CardRangeDataDTO cardRangeEntityToDTO(CardRangeEntity cardRange) {
        CardRangeDataDTO cardRangeDataDTO = new CardRangeDataDTO();
        cardRangeDataDTO.setStartRange(cardRange.getStartRange());
        cardRangeDataDTO.setEndRange(cardRange.getEndRange());
        cardRangeDataDTO.setActionInd(cardRange.getActionInd());
        cardRangeDataDTO.setThreeDsMethodUrl(cardRange.getThreeDsMethodUrl());
        cardRangeDataDTO.setAcsEndProtocolVersion(cardRange.getAcsEndProtocolVersion());
        cardRangeDataDTO.setAcsStartProtocolVersion(cardRange.getAcsStartProtocolVersion());
        cardRangeDataDTO.setAcsInfoInd(cardRange.getAcsInfoInd());

        return cardRangeDataDTO;
    }

}
