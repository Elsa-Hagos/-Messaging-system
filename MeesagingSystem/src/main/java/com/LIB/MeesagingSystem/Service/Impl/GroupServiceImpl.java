package com.LIB.MeesagingSystem.Service.Impl;


import com.LIB.MeesagingSystem.Dto.SecurityDtos.LdapUserDTO;
import com.LIB.MeesagingSystem.Model.Enums.RecipientTypes;
import com.LIB.MeesagingSystem.Model.FilePrivilege;
import com.LIB.MeesagingSystem.Model.Group;
import com.LIB.MeesagingSystem.Model.Message;
import com.LIB.MeesagingSystem.Model.Users;
import com.LIB.MeesagingSystem.Repository.FilePrivilegeRepository;
import com.LIB.MeesagingSystem.Repository.GroupRepository;
import com.LIB.MeesagingSystem.Repository.MessageRepository;
import com.LIB.MeesagingSystem.Repository.UserRepository;
import com.LIB.MeesagingSystem.Service.GroupService;
import com.LIB.MeesagingSystem.Utils.FileUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.*;

/**
 *
 *  @author Abenezer Teshome  - Date 17/aug/2024
 *  This service implementation class creates new groups, add members to the groups,
 *  create messages to groups and send messages
 */


@Service
@RequiredArgsConstructor
public class GroupServiceImpl implements GroupService {
    private static final Logger logger = LoggerFactory.getLogger(GroupServiceImpl.class);

    private final GroupRepository groupRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final FilePrivilegeRepository filePrivilegeRepository;
    @Value("${file.storage-path}")
    private String storagePath;


    public Group createGroup(Group group) {
        // Ensure all members exist in the system
        for (String memberId : group.getMembers()) {
            LdapUserDTO user = (LdapUserDTO) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            String sender = user.getUid();
            group.setMakerId(sender);
            group.setCreationDate(new Date());
            if (!userRepository.existsById(memberId)) {
                throw new RuntimeException("User with ID: " + memberId + " does not exist in the system");
            }
        }
        return groupRepository.save(group);
    }

    public Group addMembers(String groupId, List<String> memberIds) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        List<String> existingMembers = group.getMembers();
        List<String> newMembers = new ArrayList<>();

        for (String memberId : memberIds) {
            // Check if the user exists in the system
            if (!userRepository.existsById(memberId)) {
                throw new RuntimeException("User with ID " + memberId + " does not exist");
            }

            // Check if the user is already a member of the group
            if (existingMembers.contains(memberId)) {
                throw new RuntimeException("User with ID " + memberId + " is already a member of the group");
            } else {
                newMembers.add(memberId);
            }
        }

        group.getMembers().addAll(newMembers);
        return groupRepository.save(group);
    }


    public Message createGroupMessage(String senderEmail, String groupId, String content, List<MultipartFile> attachments) {
        // Find sender by email
        LdapUserDTO user = (LdapUserDTO) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Optional<Users> sender = userRepository.findByEmail(user.getEmail());
        // Find group by ID
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        if(sender.isEmpty()){
            throw new RuntimeException("Sender Email not found");
        }
        // Handle attachments
        List<String> attachmentPaths = new ArrayList<>();
        if (attachments != null && !attachments.isEmpty()) {
            for (MultipartFile attachment : attachments) {
                String filePath = FileUtils.saveAttachment(attachment, storagePath);
                attachmentPaths.add(filePath);
            }
        }

        // Create message
        Message message = new Message();
        message.setSenderId(sender.get().getId());
        message.setGroupId(groupId);
        message.setContent(content);
        message.setDraft(true);
        message.setRecipientType(RecipientTypes.GROUP);
        message.setAttachments(attachmentPaths);
        message.setDate(new Date());

        // Save message and return
        return messageRepository.save(message);
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
        saveFilePrivilegesForGroupMessage(message);
        return messageRepository.save(message);
    }
    private void saveFilePrivilegesForGroupMessage(Message message) {
        List<FilePrivilege> filePrivileges = new ArrayList<>();
        String messageId = message.getId();

        for (String attachment : message.getAttachments()) {
            FilePrivilege privilege = new FilePrivilege();
            privilege.setMessageId(messageId);
            privilege.setAttachmentId(attachment); // Assuming attachment is the file name
            //privilege.setUserId(message.getGroupId());// Assign the receiver as a user with privileges
            privilege.setGroupId(message.getGroupId());
            privilege.setCanView(true);
            privilege.setCanDownload(false); // Default to not allowing download
            filePrivileges.add(privilege);
        }

        // Save all privileges to the repository
        filePrivilegeRepository.saveAll(filePrivileges);
    }

    public Group findGroupById(String groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found with id: " + groupId));
    }
    public Group removeMemberFromGroup(String groupId, String memberId) {
        Group group = findGroupById(groupId);
        group.getMembers().remove(memberId);
        return groupRepository.save(group);
    }
}
