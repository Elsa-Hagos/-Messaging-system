package com.LIB.MeesagingSystem.Repository;

import com.LIB.MeesagingSystem.Model.Message;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 *
 *  @author Abenezer Teshome  - Date 17/aug/2024
 */

import java.util.List;
@Repository
public interface MessageRepository extends MongoRepository<Message, String> {
    List<Message> findBySenderIdAndReceiverId(String senderId, String receiverId);
    //public Message findById(ObjectId id);
    //List<Message> findByUsername(String senderUsername, String receiverUsername);
    List<Message> findByReceiverId(String receiverId);
    //Optional<Message> findBydocumentId(String documentId);
    Message deleteMessageById(String id);
    Message findByAttachmentsContaining(String fileName);
    Message findByGroupIdAndAttachmentsContaining(String groupId, String fileName);
}
