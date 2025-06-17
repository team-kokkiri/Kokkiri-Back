package com.example.kokkiri.chat.repository;

import com.example.kokkiri.chat.domain.ChatInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface ChatInvitationRepository extends JpaRepository<ChatInvitation, Long> {
}
