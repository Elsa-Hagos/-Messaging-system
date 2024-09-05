package com.LIB.MeesagingSystem.Service;

import com.LIB.MeesagingSystem.Model.EmailHistory;
import com.LIB.MeesagingSystem.Repository.EmailHistoryRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Service class for managing email history operations.
 * <p>
 * The {@link EmailHistoryService} class provides business logic and operations related to email history.
 * It interacts with the {@link EmailHistoryRepo} repository to perform CRUD operations on the email history records.
 * </p>
 *
 * @author Elizabeth Hagos
 */

@Service
public class EmailHistoryService {

    @Autowired
    private EmailHistoryRepo emailHistoryRepository;


    public List<EmailHistory> getEmailHistoriesByBoardSecretaryId(String boardSecretaryId) {
        return emailHistoryRepository.findByBoardSecretaryId(boardSecretaryId);
    }



    public List<EmailHistory> getEmailHistories(String boardSecretaryId, Date fromDate, Date toDate) {
        // Set end time to 23:59:59 for the toDate
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(toDate);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        Date endOfDay = calendar.getTime();

        return emailHistoryRepository.findByBoardSecretaryIdAndSentDateBetween(boardSecretaryId, fromDate, endOfDay);
    }


//    public List<EmailHistory> getEmailHistories(String boardSecretaryId, Date fromDate, Date toDate) {
//
//        return emailHistoryRepository.findByBoardSecretaryIdAndSentDateBetween(boardSecretaryId, fromDate, toDate);
//    }


}
