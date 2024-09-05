package com.LIB.MeesagingSystem.Controller;


import com.LIB.MeesagingSystem.Dto.GroupMessageDto;
import com.LIB.MeesagingSystem.Model.Group;
import com.LIB.MeesagingSystem.Model.Message;
import com.LIB.MeesagingSystem.Service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 *
 *  @author Abenezer Teshome  - Date 17/aug/2024
 */


@RestController
@RequestMapping("/groups")
@RequiredArgsConstructor
public class GroupController {
    private final GroupService groupServiceImpl;

    @PostMapping("/create")
    public ResponseEntity<?> createGroup(@RequestBody Group group) {
        try {
            Group createdGroup = groupServiceImpl.createGroup(group);
            return ResponseEntity.ok(createdGroup);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PutMapping("/{groupId}/addMembers")
    public Group addMembers(@PathVariable String groupId, @RequestBody List<String> memberIds) {
        return groupServiceImpl.addMembers(groupId, memberIds);
    }
    @PostMapping("/createMessage")
    public Message createGroupMessage(@ModelAttribute GroupMessageDto message) {
        return groupServiceImpl.createGroupMessage(message.getSenderEmail(),
                message.getGroupId(),
                message.getContent(),
                message.getAttachments());

    }
    @PutMapping("/send/{groupId}")
    public ResponseEntity<?> sendMessage(@PathVariable("groupId") String groupId) {
        try {
            Message sentMessage = groupServiceImpl.sendMessage(groupId);
            return ResponseEntity.ok(sentMessage);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
    @GetMapping("/search/{groupId}")
    public ResponseEntity<Group> getGroupById(@PathVariable String groupId) {
        Group group = groupServiceImpl.findGroupById(groupId);
        return ResponseEntity.ok(group);
    }
    @DeleteMapping("/removeMember")
    public ResponseEntity<String> deleteMemberFromGroup(@RequestParam String groupId, @RequestParam String memberId) {
        groupServiceImpl.removeMemberFromGroup(groupId, memberId);
        return ResponseEntity.ok("group Deleted Successfully");
    }
}

