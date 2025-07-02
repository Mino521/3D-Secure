package com.jamiewang.secure3d.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import lombok.Data;

import java.util.List;

@Data
public class PResMessageDTO {

    @JsonProperty("serialNum")
    private String serialNum;

    @JsonProperty("messageType")
    private String messageType;

    @JsonProperty("dsTransID")
    private String dsTransId;

    @JsonProperty("cardRangeData")
    @Valid
    private List<CardRangeDataDTO> cardRangeData;
}
