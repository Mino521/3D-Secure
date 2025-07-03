package com.jamiewang.secure3d.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "cached_record")
@Data
public class CachedRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pan", nullable = false)
    private Long pan;

    @Column(name = "start_range", nullable = false)
    private Long startRange;

    @Column(name = "end_range", nullable = false)
    private Long endRange;

    @Column(name = "is_valid", nullable = false)
    private Integer isValid;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

}
