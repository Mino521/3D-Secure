package com.jamiewang.secure3d.repository;

import com.jamiewang.secure3d.entity.CardRangeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ICardRangeRepository extends JpaRepository<CardRangeEntity, Long> {

    /**
     * Find the card range that contains the given PAN
     *
     * @param pan The Primary Account Number as Long to search for
     * @return Optional containing the matching card range, empty if not found
     */
    @Query("SELECT cr FROM CardRangeEntity cr WHERE :pan >= cr.startRange AND :pan <= cr.endRange")
    Optional<CardRangeEntity> findByPanInRange(@Param("pan") Long pan);

    /**
     * Find all card ranges ordered by start range for efficient loading
     * Used for building in-memory data structures
     *
     * @return List of all card ranges ordered by start range
     */
//    @Query("SELECT cr FROM CardRangeEntity cr ORDER BY cr.startRange")
//    List<CardRangeEntity> findAllOrderedByStartRange();

}
