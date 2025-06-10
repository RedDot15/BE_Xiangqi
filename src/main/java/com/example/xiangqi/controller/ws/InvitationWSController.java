package com.example.xiangqi.controller.ws;

import com.example.xiangqi.dto.request.*;
import com.example.xiangqi.service.my_sql.InvitationService;
import jakarta.validation.Valid;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;


@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Controller
public class InvitationWSController {
    InvitationService invitationService;

    @MessageMapping("/invitation.create")
    public void createInvitation(@Valid InvitationCreateRequest request) {
        // Send invitation
        invitationService.invite(request);
    }

    @MessageMapping("/invitation.accept")
    public void acceptInvitation(@Valid InvitationAcceptRequest request) {
        // Accept invitation
        invitationService.acceptInvitation(request);
    }

    @MessageMapping("/invitation.reject")
    public void rejectInvitation(@Valid InvitationRejectRequest request) {
        // Reject invitation
        invitationService.rejectInvitation(request);
    }

    @MessageMapping("/invitation.retrieve")
    public void retrieveInvitation(@Valid InvitationRetrieveRequest request) {
        // Retrieve invitation
        invitationService.retrieveInvitation(request);
    }
}