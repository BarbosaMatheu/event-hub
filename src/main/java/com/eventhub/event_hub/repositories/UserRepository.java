package com.eventhub.event_hub.repositories;

import com.eventhub.event_hub.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;


public interface UserRepository extends JpaRepository<User, String> {
    UserDetails findByLogin(String login);
}