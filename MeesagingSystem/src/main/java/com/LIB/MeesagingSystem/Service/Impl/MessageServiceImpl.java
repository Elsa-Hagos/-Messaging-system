package com.LIB.MeesagingSystem.Service.Impl;

import com.LIB.MeesagingSystem.Dto.SecurityDtos.LdapUserDTO;
import com.LIB.MeesagingSystem.Model.Enums.RecipientTypes;
import com.LIB.MeesagingSystem.Model.FilePrivilege;
import com.LIB.MeesagingSystem.Model.Message;
import com.LIB.MeesagingSystem.Model.Users;
import com.LIB.MeesagingSystem.Repository.FilePrivilegeRepository;
import com.LIB.MeesagingSystem.Repository.MessageRepository;
import com.LIB.MeesagingSystem.Repository.UserRepository;
import com.LIB.MeesagingSystem.Service.MessageService;
import com.LIB.MeesagingSystem.Utils.FileUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 *
 *  @author Abenezer Teshome  - Date 17/aug/2024
 *  This service Implementation class creates messages for Individuals, sends messages,
 *  Creates and Updates privilages for users and deletes messages
 */



@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {
    private static final Logger logger = LoggerFactory.getLogger(MessageServiceImpl.class);
    //LdapUserDTO user = (LdapUserDTO) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final FilePrivilegeRepository filePrivilegeRepository;
    @Value("${file.storage-path}")
    private String storagePath;

    public Message createMessage(String receiverEmail, String content, List<MultipartFile> attachments) {
        // Find sender and receiver
        LdapUserDTO user = (LdapUserDTO) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Optional<Users> sender = userRepository.findByEmail(user.getEmail());
        Optional<Users> receiver = userRepository.findByEmail(receiverEmail);

        if (sender.isEmpty() || receiver.isEmpty()) {
            throw new RuntimeException("Sender or receiver not found");
        }// Handle attachments
        List<String> attachmentPaths = new ArrayList<>();
        if (attachments != null && !attachments.isEmpty()) {
            if (attachments.size() > 2) {
                String zipFilePath = compressAttachments(attachments);
                attachmentPaths.add(zipFilePath);
            }else {
                for (MultipartFile attachment : attachments) {
                    String filePath = FileUtils.saveAttachment(attachment, storagePath);
                    attachmentPaths.add(filePath);
                }
            }
//            for (MultipartFile attachment : attachments) {
//                String filePath = FileUtils.saveAttachment(attachment, storagePath);
//                attachmentPaths.add(filePath);
//            }
        }
        // Create message
        Message message = new Message();
        message.setSenderId(sender.get().getId());
        message.setReceiverId(receiver.get().getId());
        message.setContent(content);
        message.setDraft(true);
        message.setRecipientType(RecipientTypes.INDIVIDUAL);
        message.setAttachments(attachmentPaths);
        message.setDate(new Date());

        // Save message and return
        Message savedMessage = messageRepository.save(message);
        System.out.println("Message saved with ID: " + savedMessage.getId());
        return savedMessage;
    }
    public Message sendMessage(String id) {
        Message message = messageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        if (!message.isDraft()) {
            throw new RuntimeException("Message is Already Sent and cannot be sent again");
        }
        // Mark the message as sent
        message.setDraft(false);
        message.setDate(new Date());
        // Save file privileges
        saveFilePrivilegesForMessage(message);
        return messageRepository.save(message);
    }

    private void saveFilePrivilegesForMessage(Message message) {
        List<FilePrivilege> filePrivileges = new ArrayList<>();
        String messageId = message.getId();
        for (String attachment : message.getAttachments()) {
            FilePrivilege privilege = new FilePrivilege();
            privilege.setMessageId(messageId);
            privilege.setAttachmentId(attachment); // Assuming attachment is the file name
            privilege.setUserId(message.getReceiverId());// Assign the receiver as a user with privileges
            privilege.setCanView(true);
            privilege.setCanDownload(true); // Default to not allowing download
            filePrivileges.add(privilege);
        }

        // Save all privileges to the repository
        filePrivilegeRepository.saveAll(filePrivileges);
    }

    public Message getMessage(String id) {
        return messageRepository.findById(id).orElseThrow(() -> new RuntimeException("Message not found"));
    }
    public List<Message> getMessagesBetweenUsers(String senderId, String receiverId){
        return messageRepository.findBySenderIdAndReceiverId(senderId, receiverId);
    }
    public List<Message> getMessagesForUser(){
        LdapUserDTO user = (LdapUserDTO) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String receiverId = user.getUid();
        return messageRepository.findByReceiverId(receiverId);
    }
    public Message findMessageByIdForUser(String messageId, String username) {
        Users user = userRepository.findByUsername(username);

        return messageRepository.findById(messageId)
                .filter(message -> message.getSenderId().equals(user.getId()) ||
                        message.getReceiverId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Message not found or access denied"));
    }
    public Message getMessageWithAttachmentCheck(String messageId, String attachmentId, String userId) {
        Optional<FilePrivilege> privilege = filePrivilegeRepository.findByMessageIdAndAttachmentIdAndUserId(messageId, attachmentId, userId);
        if (privilege.isPresent()) {
            return messageRepository.findById(messageId)
                    .orElseThrow(() -> new RuntimeException("Message not found"));
        } else {
            throw new RuntimeException("Access Denied");
        }
    }
    public Message deleteMessageById(String id){
        return messageRepository.deleteMessageById(id);
    }
    public String compressAttachments(List<MultipartFile> attachments) {
        // Generate a unique name for the zip file
        String zipFileName = "attachments_" + UUID.randomUUID().toString() + ".zip";
        Path zipFilePath = Paths.get(storagePath, zipFileName);

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFilePath.toFile()))) {
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
        } catch (IOException e) {
            throw new RuntimeException("Failed to compress attachments", e);
        }

        // Return only the zip file name, not the full path
        return zipFileName;
    }


}
