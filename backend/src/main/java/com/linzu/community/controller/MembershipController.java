package com.linzu.community.controller;

import com.linzu.common.api.Result;
import com.linzu.common.security.AuthContext;
import com.linzu.community.dto.MembershipResponse;
import com.linzu.community.service.CommunityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "社区成员关系")
@RestController
@RequestMapping({"/api/v1/memberships", "/api/memberships"})
@RequiredArgsConstructor
public class MembershipController {

    private final CommunityService communityService;

    @Operation(summary = "我的社区成员关系")
    @GetMapping("/mine")
    public Result<List<MembershipResponse>> mine() {
        return Result.ok(communityService.myMemberships(AuthContext.requireUserId()));
    }
}
