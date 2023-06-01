package com.binar.securityspringboot.service;

import com.binar.securityspringboot.model.*;
import com.binar.securityspringboot.repository.BookingRepository;
import com.binar.securityspringboot.repository.MovieRepository;
import com.binar.securityspringboot.repository.ScheduleRepository;
import com.binar.securityspringboot.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.LocalTime;
import java.util.*;

@Service
@Slf4j
public class BookingServiceImpl {

    @Autowired
    private BookingRepository repository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ScheduleRepository scheduleRepository;
    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private FirebaseMessagingService service;

    public String printReport(String bookingCode) throws FileNotFoundException, JRException {
//        String path = "D:\\code\\.springboot\\challenge 5 - Backup\\jasper";
        String path = "C:\\Users\\USER\\Documents\\jasper";

        Collection<Booking> isBookingCodeValid = repository.findByCodeBooking(bookingCode);

        if (isBookingCodeValid.isEmpty()) throw new FileNotFoundException("code booking tidak ditemukan");

        File file = ResourceUtils.getFile("classpath:jasper-report.jrxml");
        JasperReport jasperReport = JasperCompileManager.compileReport(file.getAbsolutePath());
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(isBookingCodeValid);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("createdBy", "Lucky Alma");
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
        JasperExportManager.exportReportToPdfFile(jasperPrint, path + "\\" + bookingCode + ".pdf");
        return "report generate in path: " + path;
    }

    private String getRand() {
        String saltChar = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        StringBuilder salt = new StringBuilder();
        Random rand = new Random();
        while (salt.length() < 6) {
            int index = (int) (rand.nextFloat() * saltChar.length());
            salt.append(saltChar.charAt(index));
        }

        return salt.toString();
    }


    public Booking postBooking(Long movieId, Long scheduleId, String[] seats) {

        Movie movieDoesNotExists = movieRepository.findById(movieId).orElseThrow(() -> new IllegalArgumentException("movie does not exists"));

        Schedule scheduleTemp = scheduleRepository.findByScheduleId(scheduleId);

        String bookingCode = getRand().toUpperCase();

        Booking booking = null;

        if (seats.length > 1) {
            for (String seat : seats) {

                Collection<Booking> andSeat = repository.findBookingByMovieIdAndScheduleIdAndSeat(movieId, scheduleId, seat);

                if (!andSeat.isEmpty()) throw new IllegalArgumentException("seat " + seat + " reserved");

                booking = new Booking(
                        bookingCode,
                        movieId,
                        movieDoesNotExists.getMovieName(),
                        booking.getFcmToken(),
                        scheduleId,
                        SecurityContextHolder.getContext().getAuthentication().getName().toString(),
                        scheduleTemp.getStudio(),
                        scheduleTemp.getStartTime(),
                        scheduleTemp.getEndTime(),
                        String.valueOf(scheduleTemp.getDate()),
                        seat,
                        scheduleTemp.getPrice(),
                        Status.ON_PROCESS_PAYMENT
                );
                repository.save(booking);
            }
        } else {

            Collection<Booking> andSeat = repository.findBookingByMovieIdAndScheduleIdAndSeat(movieId, scheduleId, seats[0]);

            if (!andSeat.isEmpty()) throw new IllegalArgumentException("seat " + seats[0] + " reserved");

            booking = new Booking(bookingCode, movieId, movieDoesNotExists.getMovieName(), booking.getFcmToken(), scheduleId, SecurityContextHolder.getContext().getAuthentication().getName().toString(),
                    scheduleTemp.getStudio(), scheduleTemp.getStartTime(), scheduleTemp.getEndTime(), String.valueOf(scheduleTemp.getDate()),
                    seats[0], scheduleTemp.getPrice(), Status.ON_PROCESS_PAYMENT);
            repository.save(booking);
        }

        return booking;
    }

    /*
        todo:
            TUGAS CHALLENGE 7
                membuat sebuah schedular dan juga menerapkan @Transactional yang dimana
                saya membuat sebuah pushNotification yang nantinya akan menggunakan @Transactional
                untuk melakukan pencegahan atau roll back apabila nantinya ada kesalahan dalam program
                dan juga menggunakan @Scheduled(cron = "0/10 * 9-23 * * *") sebagai trigger yang dimana
                akan menjalankan program setiap 10 detik sekali dan akan dimulai setiap hari dengan
                rentang jam 09:00 pagi sampai 11:00 malam yang akan dijalankan selamanya
                (selama program tidak mengalami kerusakan)
     */
    @Transactional
    @Scheduled(cron = "0/10 * 9-23 * * *")
    public void pushNotification() {
        log.info("**== PUSH NOTIFICATION ==**");

        List<Booking> tempNotification = repository.findByNotificationTrue();

        tempNotification.forEach(notification -> {
            if (notification.getStartTime().toLocalTime().isBefore(LocalTime.now().minusMinutes(30))) {

                // dilanjutkan dengan memberikan notifikasi menggunakan firebase yang akan melalui firecloud
                NotificationMessage message = new NotificationMessage(
                        notification.getFcmToken(),
                        "TITLE",
                        notification.getUsername() + "film anda akan segera dimulai"
                );

                // pesan akan dikirimkan yang dimana akan berisi mengenai token FCM, title, body
                service.sendNotificationByToken(message);
                repository.updateNotificationStatus(notification.getBookingId());
            }
        });
    }

}

















