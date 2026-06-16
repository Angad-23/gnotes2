package com.tutornotes.repository;

import com.tutornotes.model.Standard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StandardRepository extends JpaRepository<Standard, Long> {

    List<Standard> findByStateOrderByGradeAscTopicAsc(String state);

    List<Standard> findByStateAndGradeOrderByTopicAsc(String state, String grade);

    Optional<Standard> findByStateAndGradeAndTopic(String state, String grade, String topic);

    @Query("SELECT DISTINCT s.state FROM Standard s ORDER BY s.state")
    List<String> findDistinctStates();

    @Query("SELECT DISTINCT s.grade FROM Standard s WHERE s.state = :state ORDER BY s.grade")
    List<String> findDistinctGradesByState(String state);
}
