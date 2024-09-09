package com.LIB.MeesagingSystem.Service;


import com.LIB.MeesagingSystem.Dto.ApiResponse;
import com.LIB.MeesagingSystem.Dto.BODGroupRequest;
import com.LIB.MeesagingSystem.Model.BODGroup;
import com.LIB.MeesagingSystem.Model.BODMembers;
import com.LIB.MeesagingSystem.Repository.BODGroupRepo;
import com.LIB.MeesagingSystem.Repository.BODMembersRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class BODGroupService {

    @Autowired
    private BODGroupRepo bodGroupRepository;

    @Autowired
    private BODMembersRepo bodMembersRepository;



    public List<BODGroup> getAllBODGroups() {
        return bodGroupRepository.findAll();
    }

    public BODGroup createGroup(BODGroupRequest groupRequest) {
        BODGroup newGroup = new BODGroup();
        newGroup.setGroupName(groupRequest.getGroupName());
        newGroup.setCreatedDate(new Date());
      // newGroup.setMakerId();
        List<String> memberIds = groupRequest.getMemberIds();
        List<BODMembers> members = bodMembersRepository.findAllById(memberIds);
        newGroup.setMembers(members);
        return bodGroupRepository.save(newGroup);
    }



    public Optional<BODGroup> findGroupByName(String groupName) {
        return bodGroupRepository.findByGroupName(groupName);
    }

    /**
     * Updates an existing BOD group with new details.
     *
     * @param id   the name of the group to update.
     * @param groupRequest the request body containing updated details for the group.
     * @return a {@link ResponseEntity} containing the updated {@link BODGroup} and HTTP status 200 OK,
     *         or HTTP status 404 Not Found if the group does not exist.
     */

    public ApiResponse updateGroup(String id, BODGroup groupRequest) {
        Optional<BODGroup> existingGroup = bodGroupRepository.findById(id);
        if (existingGroup.isPresent()) {
            BODGroup group = existingGroup.get();
            group.setGroupName(groupRequest.getGroupName());
            group.setMakerId(groupRequest.getMakerId());
            group.setUpdatedDate(new Date());
            if (groupRequest.getMembers() != null && !groupRequest.getMembers().isEmpty()) {
                List<BODMembers> existingMembers = group.getMembers();
                List<BODMembers> newMembers = bodMembersRepository.findAllById(groupRequest.getMembers());
                for (BODMembers newMember : newMembers) {
                    if (!existingMembers.contains(newMember)) {
                        existingMembers.add(newMember);
                    }
                }
                group.setMembers(existingMembers);
            }

            bodGroupRepository.save(group);
            return new ApiResponse("Success", "group updated successfully.");

        }
        else
        return new ApiResponse("Error", "group not found.");
    }


//    public Optional<BODGroup> updateGroup(String id, BODGroup groupRequest) {
//        Optional<BODGroup> existingGroup = bodGroupRepository.findById(id);
//        if (existingGroup.isPresent()) {
//            BODGroup group = existingGroup.get();
//            group.setGroupName(groupRequest.getGroupName());
//            group.setMakerId(groupRequest.getMakerId());
//            group.setUpdatedDate(new Date());
//            if (groupRequest.getMembers() != null && !groupRequest.getMembers().isEmpty()) {
//                List<BODMembers> members = bodMembersRepository.findAllById(groupRequest.getMembers());
//                group.setMembers(members);
//            }
//
//            bodGroupRepository.save(group);
//            return Optional.of(group);
//        }
//        return Optional.empty();
//    }

    /**
     * Deletes a BOD group by its name.
     *
     * @param id the name of the group to delete.
     * @return a {@link ResponseEntity} with HTTP status 204 No Content if the group was successfully deleted,
     *         or HTTP status 404 Not Found if the group does not exist.
     */

    public boolean deleteGroup(String id) {
        Optional<BODGroup> group = bodGroupRepository.findById(id);
        if (group.isPresent()) {
            bodGroupRepository.delete(group.get());
            return true;
        }
        return false;
    }

    public BODGroup getBODGroupById(String id) {
        Optional<BODGroup> bodGroupOptional = bodGroupRepository.findById(id);
        return bodGroupOptional.orElse(null);
    }

    /**
     * Adds members to a BOD group.
     *
     * @param groupName the name of the group to which members will be added.
     * @param memberIds the list of member IDs to add to the group.
     * @return a {@link ResponseEntity} containing the updated {@link BODGroup} and HTTP status 200 OK,
     *         or HTTP status 404 Not Found if the group does not exist.
     */

    public Optional<BODGroup> addMembersToGroup(String groupName, List<String> memberIds) {
        Optional<BODGroup> existingGroup = bodGroupRepository.findByGroupName(groupName);
        if (existingGroup.isPresent()) {
            BODGroup group = existingGroup.get();
            List<BODMembers> membersToAdd = bodMembersRepository.findAllById(memberIds);
            List<BODMembers> currentMembers = group.getMembers();
            Set<BODMembers> currentMembersSet = new HashSet<>(currentMembers);
            List<BODMembers> newMembers = membersToAdd.stream()
                    .filter(member -> !currentMembersSet.contains(member))
                    .collect(Collectors.toList());
            currentMembers.addAll(newMembers);
            group.setMembers(currentMembers);
            bodGroupRepository.save(group);

            return Optional.of(group);
        }
        return Optional.empty();
    }

    /**
     * Removes members from a BOD group.
     *
     * @param groupName the name of the group from which members will be removed.
     * @param memberIds the list of member IDs to remove from the group.
     * @return a {@link ResponseEntity} containing the updated {@link BODGroup} and HTTP status 200 OK,
     *         or HTTP status 404 Not Found if the group does not exist.
     */

    public Optional<BODGroup> removeMembersFromGroup(String groupName, List<String> memberIds) {
        Optional<BODGroup> existingGroup = bodGroupRepository.findByGroupName(groupName);
        if (existingGroup.isPresent()) {
            BODGroup group = existingGroup.get();
            List<BODMembers> currentMembers = group.getMembers();
            List<BODMembers> membersToRemove = bodMembersRepository.findAllById(memberIds);
            currentMembers.removeAll(membersToRemove);
            group.setMembers(currentMembers);
            bodGroupRepository.save(group);
            return Optional.of(group);
        }
        return Optional.empty();
    }


}



