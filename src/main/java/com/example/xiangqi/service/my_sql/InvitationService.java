package com.example.xiangqi.service.my_sql;


import com.example.xiangqi.dto.request.InvitationAcceptRequest;
import com.example.xiangqi.dto.request.InvitationCreateRequest;
import com.example.xiangqi.dto.request.InvitationRejectRequest;
import com.example.xiangqi.dto.request.InvitationRetrieveRequest;
import com.example.xiangqi.exception.AppException;
import com.example.xiangqi.exception.ErrorCode;
import com.example.xiangqi.helper.MessageObject;
import jakarta.transaction.Transactional;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Transactional
@Service
public class InvitationService {
    // Maps to store invitations
    Map<String, List<String>> invitations = new ConcurrentHashMap<>();

    MatchService matchService;
    OnlinePlayerService onlinePlayerService;
    SimpMessagingTemplate messagingTemplate;

    public void invite(InvitationCreateRequest request) {
        // Find player in session list
        OnlinePlayerService.PlayerInfo inviteePlayerInfo = onlinePlayerService.getPlayerInfo(request.getInviteeUsername());
        OnlinePlayerService.PlayerInfo inviterPlayerInfo = onlinePlayerService.getPlayerInfo(request.getInviterUsername());
        // Not found exception
        if (inviteePlayerInfo == null)
            throw new AppException(ErrorCode.USER_NOT_FOUND);

        // Opponent status invalid
        if (inviteePlayerInfo.getStatus().equals(OnlinePlayerService.PlayerStatus.IN_MATCH))
            throw new AppException(ErrorCode.OPPONENT_STATUS_IN_MATCH);
        if (inviteePlayerInfo.getStatus().equals(OnlinePlayerService.PlayerStatus.QUEUE))
            throw new AppException(ErrorCode.OPPONENT_STATUS_QUEUE);

        // Save invitation
        // Get invitee's invitation list or create new one
        invitations.computeIfAbsent(request.getInviterUsername(), k -> new CopyOnWriteArrayList<>());
        // Invitation exists exception
        if (invitations.get(request.getInviterUsername()).contains(request.getInviteeUsername())) {
            throw new AppException(ErrorCode.INVITATION_ALREADY_EXISTS); // Ngăn gửi lời mời trùng
        }
        // Add the invitation
        invitations.get(request.getInviterUsername()).add(request.getInviteeUsername());

        // Send invitation creation message
        messagingTemplate.convertAndSend("/topic/invite/player/" + inviterPlayerInfo.getPlayerId(),
                new MessageObject("To-inviter: Your invitation sent.", inviteePlayerInfo.getUsername()));
        messagingTemplate.convertAndSend("/topic/invite/player/" + inviteePlayerInfo.getPlayerId(),
                new MessageObject("To-invitee: A new invitation received.", inviterPlayerInfo.getUsername()));
    }

    public void acceptInvitation(InvitationAcceptRequest request) {
        // Get invitation list
        List<String> opponentUsernameList = invitations.get(request.getInviterUsername());
        // Remove invitation
        boolean invitationExists = opponentUsernameList.remove(request.getInviteeUsername());
        // Invitation not found exception
        if (!invitationExists)
            throw new AppException(ErrorCode.INVITATION_NOT_FOUND);

        // Get player info
        OnlinePlayerService.PlayerInfo inviterPlayerInfo = onlinePlayerService.getPlayerInfo(request.getInviterUsername());
        OnlinePlayerService.PlayerInfo inviteePlayerInfo = onlinePlayerService.getPlayerInfo(request.getInviteeUsername());

        // Create match
        Long matchId = matchService.createMatch(
                inviterPlayerInfo.getPlayerId(),
                inviteePlayerInfo.getPlayerId(),
                false);

        // Send invitation accept message
        messagingTemplate.convertAndSend("/topic/invite/player/" + inviterPlayerInfo.getPlayerId(),
                new MessageObject("To-inviter: Your invitation accepted.", matchId));
        messagingTemplate.convertAndSend("/topic/invite/player/" + inviteePlayerInfo.getPlayerId(),
                new MessageObject("To-invitee: Accept invitation success.", matchId));
    }

    public void rejectInvitation(InvitationRejectRequest request) {
        if (request.getInviterUsername() == null != request.getInviterUsername().isEmpty()) {
            for (String inviterUsername : invitations.keySet()) {
                List<String> opponentUsernameList = invitations.get(inviterUsername);
                if (opponentUsernameList.contains(request.getInviteeUsername())) {
                    // Remove invitation
                    opponentUsernameList.remove(request.getInviteeUsername());
                    // Get player info
                    OnlinePlayerService.PlayerInfo inviterPlayerInfo = onlinePlayerService.getPlayerInfo(inviterUsername);
                    OnlinePlayerService.PlayerInfo inviteePlayerInfo = onlinePlayerService.getPlayerInfo(request.getInviteeUsername());

                    // Send invitation reject message
                    messagingTemplate.convertAndSend("/topic/invite/player/" + inviterPlayerInfo.getPlayerId(),
                            new MessageObject("To-inviter: Your invitation rejected.", inviteePlayerInfo.getUsername()));
                    messagingTemplate.convertAndSend("/topic/invite/player/" + inviteePlayerInfo.getPlayerId(),
                            new MessageObject("To-invitee: Reject invitation success.", inviterPlayerInfo.getUsername()));
                }
            }
            // Return
            return;
        }

        // Get invitation list
        List<String> inviteeUsernameList = invitations.get(request.getInviterUsername());
        // Remove invitation
        boolean invitationExists = inviteeUsernameList.remove(request.getInviteeUsername());
        // Invitation not found exception
        if (!invitationExists)
            throw new AppException(ErrorCode.INVITATION_NOT_FOUND);

        // Get player info
        OnlinePlayerService.PlayerInfo inviterPlayerInfo = onlinePlayerService.getPlayerInfo(request.getInviterUsername());
        OnlinePlayerService.PlayerInfo inviteePlayerInfo = onlinePlayerService.getPlayerInfo(request.getInviteeUsername());

        // Send invitation reject message
        messagingTemplate.convertAndSend("/topic/invite/player/" + inviterPlayerInfo.getPlayerId(),
                new MessageObject("To-inviter: Your invitation rejected.", inviteePlayerInfo.getUsername()));
        messagingTemplate.convertAndSend("/topic/invite/player/" + inviteePlayerInfo.getPlayerId(),
                new MessageObject("To-invitee: Reject invitation success.", inviterPlayerInfo.getUsername()));
    }


    public void retrieveInvitation(InvitationRetrieveRequest request) {
        // Get invitation list
        List<String> inviteeUsernameList = invitations.get(request.getInviterUsername());
        if (inviteeUsernameList == null) return;

        // Delete every invitation if username null
        if (request.getInviteeUsername() == null || request.getInviteeUsername().isEmpty()) {
            for (String inviteeUsername : inviteeUsernameList) {
                // Remove invitation
                inviteeUsernameList.remove(inviteeUsername);
                // Get player info
                OnlinePlayerService.PlayerInfo inviteePlayerInfo = onlinePlayerService.getPlayerInfo(inviteeUsername);
                OnlinePlayerService.PlayerInfo inviterPlayerInfo = onlinePlayerService.getPlayerInfo(request.getInviterUsername());

                // Send invitation cancel message
                messagingTemplate.convertAndSend("/topic/invite/player/" + inviterPlayerInfo.getPlayerId(),
                        new MessageObject("To-inviter: Retrieve invitation success.", inviteePlayerInfo.getUsername()));
                messagingTemplate.convertAndSend("/topic/invite/player/" + inviteePlayerInfo.getPlayerId(),
                        new MessageObject("To-invitee: Invitation retrieved.", inviterPlayerInfo.getUsername()));
            }
            // Return
            return ;
        }

        // Remove invitation
        inviteeUsernameList.remove(request.getInviteeUsername());
        // Get player info
        OnlinePlayerService.PlayerInfo inviteePlayerInfo = onlinePlayerService.getPlayerInfo(request.getInviteeUsername());
        OnlinePlayerService.PlayerInfo inviterPlayerInfo = onlinePlayerService.getPlayerInfo(request.getInviterUsername());

        // Send invitation cancel message
        messagingTemplate.convertAndSend("/topic/invite/player/" + inviterPlayerInfo.getPlayerId(),
                new MessageObject("To-inviter: Retrieve invitation success.", inviteePlayerInfo.getUsername()));
        messagingTemplate.convertAndSend("/topic/invite/player/" + inviteePlayerInfo.getPlayerId(),
                new MessageObject("To-invitee: Invitation retrieved.", inviterPlayerInfo.getUsername()));
    }
}
