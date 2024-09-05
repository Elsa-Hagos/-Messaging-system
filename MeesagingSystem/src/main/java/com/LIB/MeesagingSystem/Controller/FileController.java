package com.LIB.MeesagingSystem.Controller;



import com.LIB.MeesagingSystem.Dto.SecurityDtos.LdapUserDTO;
import com.LIB.MeesagingSystem.Model.FilePrivilege;
import com.LIB.MeesagingSystem.Model.Message;
import com.LIB.MeesagingSystem.Repository.FilePrivilegeRepository;
import com.LIB.MeesagingSystem.Repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;


/**
 *
 *  @author Abenezer Teshome  - Date 17/aug/2024
 */

@RestController
@RequestMapping("/downloadFiles")
@RequiredArgsConstructor
public class FileController {
    @Value("${file.storage-path}")
    private String storagePath;
    private final MessageRepository messageRepository;
    private final FilePrivilegeRepository filePrivilegeRepository;

    @GetMapping("/viewAttachment")
    public ResponseEntity<?> viewAttachment(@RequestParam String fileName) throws IOException {
        // Retrieve the message by the attachment file name
        Message message = messageRepository.findByAttachmentsContaining(fileName);
        if (message == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Message not found for the given attachment");
        }

        // Validate user's access to the file
        LdapUserDTO user = (LdapUserDTO) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userId = user.getUid();
        FilePrivilege privilege = filePrivilegeRepository.findByAttachmentIdAndUserId(fileName, userId)
                .orElseThrow(() -> new RuntimeException("Access Denied"));

        if (!privilege.isCanView() && !privilege.isCanDownload()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You don't have permission to view or download this file");
        }

        Path path = Paths.get(storagePath).resolve(fileName);
        Resource resource = new UrlResource(path.toUri());

        if (resource.exists() || resource.isReadable()) {
            String contentType = Files.probeContentType(path);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            // Determine content disposition based on permissions
            String contentDisposition;
            if (privilege.isCanDownload()) {
                contentDisposition = "attachment; filename=\"" + path.getFileName().toString() + "\"";
            } else {
                contentDisposition = "inline; filename=\"" + path.getFileName().toString() + "\"";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                    .body(resource);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File not found or not readable");
        }
    }


    @PostMapping("/setFilePrivilege")
    public ResponseEntity<?> setFilePrivilege(@RequestParam String messageId, @RequestParam String attachmentId,
                                              @RequestParam String userId, @RequestParam boolean canView,
                                              @RequestParam boolean canDownload) {
        // Debugging logs
        System.out.println("Received Request: messageId=" + messageId + ", attachmentId=" + attachmentId + ", userId=" + userId);
        System.out.println("Privileges: canView=" + canView + ", canDownload=" + canDownload);

        // Check if the query returns something
        Optional<FilePrivilege> optionalPrivilege = filePrivilegeRepository.findByMessageIdAndAttachmentIdAndUserId(messageId, attachmentId, userId);
        if (optionalPrivilege.isPresent()) {
            System.out.println("Privilege found: Updating existing privilege");
        } else {
            System.out.println("Privilege not found: Creating new privilege");
        }

        FilePrivilege privilege;
        if (optionalPrivilege.isPresent()) {
            privilege = optionalPrivilege.get();
        } else {
            privilege = new FilePrivilege();
            privilege.setMessageId(messageId);
            privilege.setAttachmentId(attachmentId);
            privilege.setUserId(userId);
        }

        privilege.setCanView(canView);
        privilege.setCanDownload(canDownload);

        // Save privilege and log the operation
        filePrivilegeRepository.save(privilege);
        System.out.println("Privileges updated successfully");

        return ResponseEntity.ok("Privileges updated successfully");
    }
    @PostMapping("/group/setFilePrivilege")
    public ResponseEntity<?> setGroupFilePrivilege(@RequestParam String groupId,
                                                   @RequestParam String attachmentId,
                                                   //@RequestParam String userId,
                                                   @RequestParam boolean canView,
                                                   @RequestParam boolean canDownload) {
        Optional<FilePrivilege> optionalPrivilege = filePrivilegeRepository.findByAttachmentIdAndGroupId(groupId, attachmentId);

        FilePrivilege privilege;
        if (optionalPrivilege.isPresent()) {
            privilege = optionalPrivilege.get();
        } else {
            privilege = new FilePrivilege();
            privilege.setGroupId(groupId);
            privilege.setAttachmentId(attachmentId);
            //privilege.setUserId(userId);
        }
        privilege.setCanView(canView);
        privilege.setCanDownload(canDownload);

        filePrivilegeRepository.save(privilege);
        return ResponseEntity.ok("Group file privileges updated successfully");
    }
    @GetMapping("/group/viewAttachment")
    public ResponseEntity<Resource> viewGroupAttachment(@RequestParam String fileName, @RequestParam String groupId) throws IOException {
        Message message = messageRepository.findByGroupIdAndAttachmentsContaining(groupId, fileName);
        if (message == null) {
            throw new RuntimeException("Message not found for the given attachment");
        }
        FilePrivilege privilege = filePrivilegeRepository.findByAttachmentIdAndGroupId(fileName, groupId)
                .orElseThrow(() -> new RuntimeException("Access Denied"));

        if (!privilege.isCanView() && !privilege.isCanDownload()) {
            throw new RuntimeException("You don't have permission to view or download this file");
        }

        Path path = Paths.get(storagePath).resolve(fileName);
        Resource resource = new UrlResource(path.toUri());

        if (resource.exists() || resource.isReadable()) {
            String contentType = Files.probeContentType(path);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            String contentDisposition = privilege.isCanDownload()
                    ? "attachment; filename=\"" + path.getFileName().toString() + "\""
                    : "inline; filename=\"" + path.getFileName().toString() + "\"";

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                    .body(resource);
        } else {
            throw new RuntimeException("File not found or not readable");
        }
    }
}
