package com.agroprecision.controller;

import com.agroprecision.dto.AuthDtos.*;
import com.agroprecision.security.AuthUser;
import com.agroprecision.service.InviteService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invites")
public class InviteController {
    private final InviteService invites;

    public InviteController(InviteService invites) { this.invites = invites; }

    @PostMapping
    public InviteCreated create(@RequestBody InviteRequest req,
                                @AuthenticationPrincipal AuthUser user) {
        return invites.createInvite(req.farmId(), user.id());
    }

    @GetMapping("/{token}")
    public InvitePreview preview(@PathVariable String token) {
        return invites.previewInvite(token);
    }
}

