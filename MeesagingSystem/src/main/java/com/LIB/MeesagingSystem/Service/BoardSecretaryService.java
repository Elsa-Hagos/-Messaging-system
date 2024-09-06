package com.LIB.MeesagingSystem.Service;

import com.LIB.MeesagingSystem.Dto.ApiResponse;
import com.LIB.MeesagingSystem.Dto.BSRequest;

import com.LIB.MeesagingSystem.Model.BoardSecretary;
import com.LIB.MeesagingSystem.Repository.BODGroupRepo;
import com.LIB.MeesagingSystem.Repository.BoardSecretaryRepo;

import lombok.RequiredArgsConstructor;
import org.springframework.ldap.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BoardSecretaryService {

    private final String PHONE_NUMBER_REGEX = "^(\\+2519|\\+2517|2519|2517|002517|002519|09|07)\\d{8}$";
    private final Pattern PHONE_PATTERN = Pattern.compile(PHONE_NUMBER_REGEX);
    private final String EMAIL_REGEX = "^[\\w._%+-]+@anbesabank\\.com$";
    private final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);
    private final Set<String> combinations = new HashSet<>();
    private final PasswordEncoder bCryptPasswordEncoder;
    private final BoardSecretaryRepo boardSecretaryRepo;
    private final BODGroupRepo bodGroupRepo;

    public List<BoardSecretary> getAllBoardSecretary() {
        return boardSecretaryRepo.findAll();
    }

    public Optional<BoardSecretary> getBoardSecretaryByName(String firstName, String middleName, String lastName) {
        return boardSecretaryRepo.findByFirstNameAndMiddleNameAndLastName(firstName, middleName, lastName);
    }



    public ApiResponse createBoardSecretary(BSRequest bsRequest) {
        if (bsRequest.getFirstName() == null || bsRequest.getFirstName().isEmpty() ||
                bsRequest.getMiddleName() == null || bsRequest.getMiddleName().isEmpty() ||
                bsRequest.getLastName() == null || bsRequest.getLastName().isEmpty()) {
            return new ApiResponse("Error", "First name, middle name, and last name are required.");
        }
        if (bsRequest.getEmail() == null || !EMAIL_PATTERN.matcher(bsRequest.getEmail()).matches()) {
            return new ApiResponse("Error", "Invalid email format.");
        }
        if (bsRequest.getMobile() == null || !PHONE_PATTERN.matcher(bsRequest.getMobile()).matches()) {
            return new ApiResponse("Error", "Invalid phone number.");
        }

        loadExistingCombinations();
        String combinationKey = bsRequest.getFirstName() + ":" + bsRequest.getMiddleName() + ":" + bsRequest.getLastName();
        if (combinations.contains(combinationKey)) {
            return new ApiResponse("Error", "Board Secretary with this name combination already exists.");
        }

        Optional<BoardSecretary> existingEmail = boardSecretaryRepo.findByEmail(bsRequest.getEmail());
        if (existingEmail.isPresent()) {
            return new ApiResponse("Error", "Board Secretary with this email already exists.");
        }
        String encodedPassword = bCryptPasswordEncoder.encode(bsRequest.getPassword());
        BoardSecretary boardSecretary = convertToEntity(bsRequest, encodedPassword);
        boardSecretaryRepo.save(boardSecretary);
        combinations.add(combinationKey);
        return new ApiResponse("Success", "Board Secretary created successfully.");
    }

    private BoardSecretary convertToEntity(BSRequest bsRequest, String encodedPassword) {
        BoardSecretary boardSecretary = new BoardSecretary();
        boardSecretary.setFirstName(bsRequest.getFirstName());
        boardSecretary.setMiddleName(bsRequest.getMiddleName());
        boardSecretary.setLastName(bsRequest.getLastName());
        boardSecretary.setEmail(bsRequest.getEmail());
        boardSecretary.setMobile(bsRequest.getMobile());
        boardSecretary.setPassword(encodedPassword);
        boardSecretary.setGroupID(bsRequest.getGroupID());
        boardSecretary.setActive(bsRequest.isActive());

        boardSecretary.setCreatedDate(new Date());
        boardSecretary.setUpdatedDate(new Date());

        return boardSecretary;
    }

    public ApiResponse deleteBoardSecretaryById(String id) {
        Optional<BoardSecretary> bs = boardSecretaryRepo.findById(id);
        if (bs.isPresent()) {
            boardSecretaryRepo.delete(bs.get());
            return new ApiResponse("Success", "Board Secretary deleted");
        } else {
            return new ApiResponse("Error", "Board Secretary not found");
        }}


    public ApiResponse updateBoardSecretary(String id, BSRequest bSRequest) {

        Optional<BoardSecretary> boardSecretaryOptional = boardSecretaryRepo.findById(id);
        if (boardSecretaryOptional.isPresent()) {
            if (bSRequest.getEmail() == null || !EMAIL_PATTERN.matcher(bSRequest.getEmail()).matches()) {
                return new ApiResponse("Error", "Invalid email format.");}
                BoardSecretary existingBoardSecretary = boardSecretaryOptional.get();
            Date createdDate = existingBoardSecretary.getCreatedDate();
            String encodedPassword = bCryptPasswordEncoder.encode(bSRequest.getPassword());
         //   String encodedPassword = passwordEncoder.encode(bSRequest.getPassword());
            BoardSecretary updatedBoardSecretary = convertToEntity(bSRequest, encodedPassword);
            updatedBoardSecretary.setId(existingBoardSecretary.getId());
            updatedBoardSecretary.setCreatedDate(createdDate);
            updatedBoardSecretary.setUpdatedDate(new Date());
            boardSecretaryRepo.save(updatedBoardSecretary);

            return new ApiResponse("Success", "Board Secretary updated successfully.");
        } else {
            return new ApiResponse("Error", "Board Secretary not found.");
        }
    }


    private void loadExistingCombinations() {
        List<BoardSecretary> existingEmails = boardSecretaryRepo.findAll();
        combinations.clear();
        for (BoardSecretary bs : existingEmails) {
            String key = bs.getFirstName() + ":" + bs.getMiddleName() + ":" + bs.getLastName();
            combinations.add(key);
        }
    }

    public boolean externalUserLogin(String email, String password) {
        BoardSecretary boardSecretary = boardSecretaryRepo.findByEmailAndIsActive(email,true).orElseThrow(() -> new AuthenticationException());
        return bCryptPasswordEncoder.matches(password, boardSecretary.getPassword());
    }

    public BoardSecretary getExternalUser(String email, boolean active){
        return boardSecretaryRepo.findByEmailAndIsActive(email,active).orElseThrow(() -> new AuthenticationException());
    }
}
