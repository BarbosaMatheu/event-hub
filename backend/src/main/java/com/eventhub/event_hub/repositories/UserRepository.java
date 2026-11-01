package com.eventhub.event_hub.repositories;

import com.eventhub.event_hub.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByLogin(String login);

    boolean existsByLogin(String login);
}
