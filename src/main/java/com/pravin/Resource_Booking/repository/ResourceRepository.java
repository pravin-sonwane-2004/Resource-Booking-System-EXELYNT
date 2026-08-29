package com.pravin.Resource_Booking.repository;

import com.pravin.Resource_Booking.entity.Resource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResourceRepository extends JpaRepository<Resource, Long> {

    List<Resource> findByAvailableTrue();
}
