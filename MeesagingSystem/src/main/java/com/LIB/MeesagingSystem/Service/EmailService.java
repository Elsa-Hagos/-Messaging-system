package com.LIB.MeesagingSystem.Service;
import com.LIB.MeesagingSystem.Dto.ApiResponse;
import com.LIB.MeesagingSystem.Dto.SecurityDtos.LdapUserDTO;
import com.LIB.MeesagingSystem.Model.BODGroup;
import com.LIB.MeesagingSystem.Model.BODMembers;
import com.LIB.MeesagingSystem.Model.EmailHistory;
import com.LIB.MeesagingSystem.Repository.BODGroupRepo;
import com.LIB.MeesagingSystem.Repository.BODMembersRepo;
import com.LIB.MeesagingSystem.Repository.EmailHistoryRepo;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class EmailService {

    private static final String storagePath = "F:/EmailZip";
    String url = "http://10.1.7.115:9191/api/v1/lionbank/channel/smtp/send/email2";
    private static final String AUTH_USERNAME = "lion";
    private static final String AUTH_PASSWORD = "bank";
    private final RestTemplate restTemplate;
    private BODMembersRepo bodMembersRepo;
    private BODGroupRepo bodGroupRepo;
    private EmailHistoryRepo emailHistoryRepo;


    public EmailService(RestTemplate restTemplate, BODMembersRepo bodMembersRepo , BODGroupRepo bodGroupRepo , EmailHistoryRepo emailHistoryRepo) {
        this.restTemplate = restTemplate;
        this.bodMembersRepo = bodMembersRepo;
        this.bodGroupRepo = bodGroupRepo;
        this.emailHistoryRepo = emailHistoryRepo;
    }

    public ApiResponse sendToMember( List<String> memberID, String subject, String message, MultipartFile[] files , String boardSecretaryid) throws IOException {
        LdapUserDTO user = (LdapUserDTO) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<String> emails = bodMembersRepo.findByIdIn(memberID).stream()
                .map(BODMembers::getEmail)
                .toList();
        if (emails.isEmpty()) {
            return new ApiResponse("Error", "Member not found");

        }
        String from = "Lion International Bank S.C <" + user.getEmail() + ">";
      //  memberID
        for (String email : emails) {
            SendEmail(from, email, subject, message, files);
            saveEmailHistory(   emails ,null, subject ,message ,boardSecretaryid );
        }
        return new ApiResponse("Success", "Email Sent Successfully");
    }
    public ApiResponse sendToGroup( String groupId, String subject, String message , MultipartFile[] files , String boardSecretaryid) throws IOException {
        LdapUserDTO user = (LdapUserDTO) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        BODGroup group = bodGroupRepo.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found with id: " + groupId));
        List<String> memberIds = group.getMembers().stream()
                .map(BODMembers::getId)
                .collect(Collectors.toList());
        List<String> emails = bodMembersRepo.findByIdIn(memberIds).stream()
                .map(BODMembers::getEmail)
                .collect(Collectors.toList());
        if (emails.isEmpty()) {
            return new ApiResponse("Error", "Group not found");
        }
        String from = "Lion International Bank S.C <" + user.getEmail() + ">";

        for (String email : emails) {
            SendEmail(from, email, subject, message, files);
            saveEmailHistory(null, groupId,  subject ,message, boardSecretaryid );
        }
        return new ApiResponse("Success", "Email Sent Successfully");
    }
    public String SendEmail(String from, String to, String subject, String message, MultipartFile[] files) throws IOException {



        MultiValueMap<String, Object> bodyMap = new LinkedMultiValueMap<>();
        bodyMap.add("from", from);
        bodyMap.add("to", to);
        bodyMap.add("subject", subject);
        bodyMap.add("message", message);
        if (files != null) {
            String zipFileName = compressAttachments(Arrays.asList(files));
            Path zipFilePath = Paths.get(storagePath, zipFileName);

            try {
                byte[] zipFileBytes = Files.readAllBytes(zipFilePath);
                bodyMap.add("files", new ByteArrayResource(zipFileBytes) {
                    @Override
                    public String getFilename() {
                        return zipFileName;
                    }
                });
            } catch (IOException e) {
                throw new RuntimeException("Failed to read ZIP file", e);
            }
        }

//        if (files != null) {
//            for (MultipartFile file : files) {
//                bodyMap.add("files", new ByteArrayResource(file.getBytes()) {
//                    @Override
//                    public String getFilename() {
//                        return file.getOriginalFilename();
//                    }
//                });
//            }
//        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBasicAuth(AUTH_USERNAME, AUTH_PASSWORD);
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(bodyMap, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
        if (response.getStatusCode() == HttpStatus.OK) {
            return "Email sent successfully";
        } else {
            return "Email not sent, status code: " + response.getStatusCode();
        }
    }

    private void saveEmailHistory( List<String> recipient, String recipientGroupId, String subject, String message ,String boardSecretaryid) {
        EmailHistory emailHistory = new EmailHistory();
        emailHistory.setBoardSecretaryId(boardSecretaryid);
        emailHistory.setRecipient(recipient);
        emailHistory.setRecipientGroupId(recipientGroupId);
        emailHistory.setSubject(subject);
        emailHistory.setMessage(message);
        emailHistory.setSentDate(new Date());
        emailHistoryRepo.save(emailHistory);
    }



    public String compressAttachments(List<MultipartFile> attachments) {
        String zipFileName = "attachments_" + UUID.randomUUID().toString() + ".zip";
        Path zipFilePath = Paths.get(storagePath, zipFileName);
        File directory = new File(storagePath);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            for (MultipartFile attachment : attachments) {
                ZipEntry zipEntry = new ZipEntry(attachment.getOriginalFilename());
                zos.putNextEntry(zipEntry);

                try (InputStream inputStream = attachment.getInputStream()) {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = inputStream.read(buffer)) > 0) {
                        zos.write(buffer, 0, length);
                    }
                }
                zos.closeEntry();
            }
            zos.finish();

            // Save the ZIP file to the specified path
            Files.write(zipFilePath, baos.toByteArray());

            return zipFileName;
        } catch (IOException e) {
            throw new RuntimeException("Failed to compress and save attachments", e);
        }
    }

}


