package com.binar.securityspringboot.repository;

import com.binar.securityspringboot.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findBookingByMovieIdAndScheduleId(Long movieFk, Long scheduleId);

    Collection<Booking> findByCodeBooking(String bookingId);

    Collection<Booking> findBookingByMovieIdAndScheduleIdAndSeat(Long movieId, Long scheduleId, String seat);

    List<Booking> findByNotificationTrue();

    @Transactional
    @Modifying
    @Query("UPDATE Booking b " +
            "SET b.notification = false " +
            "WHERE b.bookingId = ?1")
    void updateNotificationStatus(Long bookingId);
}
