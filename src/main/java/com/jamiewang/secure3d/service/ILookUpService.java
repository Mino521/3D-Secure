package com.jamiewang.secure3d.service;

import com.jamiewang.secure3d.dto.CardRangeDataDTO;

import java.util.Optional;

public interface ILookUpService {

    /**
     * Lookup card range by PAN
     *
     * @param pan Primary Account Number
     * @return Optional containing the lookup response, empty if not found
     */
    Optional<CardRangeDataDTO> lookupByPan(Long pan);

}
