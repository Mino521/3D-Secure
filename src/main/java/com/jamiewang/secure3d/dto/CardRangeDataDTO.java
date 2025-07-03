package com.jamiewang.secure3d.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class CardRangeDataDTO {

    @JsonProperty("startRange")
    private Long startRange;

    @JsonProperty("endRange")
    private Long endRange;

    @JsonProperty("actionInd")
    private String actionInd;

    @JsonProperty("acsEndProtocolVersion")
    private String acsEndProtocolVersion;

    @JsonProperty("threeDSMethodURL")
    private String threeDsMethodUrl;

    @JsonProperty("acsStartProtocolVersion")
    private String acsStartProtocolVersion;

    @JsonProperty("acsInfoInd")
    private List<String> acsInfoInd;

}
