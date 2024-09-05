package com.LIB.MeesagingSystem.Repository;

import com.LIB.MeesagingSystem.Model.Group;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 *
 *  @author Abenezer Teshome  - Date 17/aug/2024
 */

@Repository
public interface GroupRepository extends MongoRepository<Group, String> {
    Group findByName(String name);
}
