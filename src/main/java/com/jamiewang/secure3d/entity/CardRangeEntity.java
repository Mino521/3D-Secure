package com.jamiewang.secure3d.entity;

import com.jamiewang.secure3d.util.IntervalData;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "card_range", indexes = {
        @Index(name = "idx_range_composite", columnList = "startRange, endRange")
})
@Data
public class CardRangeEntity implements IntervalData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "start_range", nullable = false)
    private Long startRange;

    @Column(name = "end_range", nullable = false)
    private Long endRange;

    @Column(name = "action_ind", length = 1)
    private String actionInd;

    @Column(name = "acs_end_protocol_version", length = 20)
    private String acsEndProtocolVersion;

    @Column(name = "three_ds_method_url", length = 2048)
    private String threeDsMethodUrl;

    @Column(name = "acs_start_protocol_version", length = 20)
    private String acsStartProtocolVersion;

    @ElementCollection
    @CollectionTable(name = "acs_info",
            joinColumns = @JoinColumn(name = "card_range_id"))
    private List<String> acsInfoInd;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Checks if a given PAN falls within this card range
     *
     * @param pan The Primary Account Number to check
     * @return true if the PAN is within the range, false otherwise
     */
    public boolean containsPan(Long pan) {
        if (pan == null) {
            return false;
        }

        return pan >= startRange && pan <= endRange;
    }
}
