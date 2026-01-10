package xyz.ncookie.stargazer.domain.spot.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import xyz.ncookie.stargazer.domain.spot.entity.ObservationSpot;

public interface ObservationRepository extends JpaRepository<ObservationSpot, Long> {
}
