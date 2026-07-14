package com.autodealer.crm.modules.identity.web;

import com.autodealer.crm.modules.identity.application.api.dto.profile.ProfileDtos.*;
import com.autodealer.crm.shared.web.Result;
import com.autodealer.crm.modules.identity.application.api.ProfileService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/profile")
public class ProfileController {
  private final ProfileService service;public ProfileController(ProfileService service){this.service=service;}
  @GetMapping public Result<Profile> get(){return Result.OK(service.getOwn());}
  @PutMapping public Result<Profile> update(@Valid @RequestBody UpdateRequest request){return Result.OK(service.updateOwn(request));}
}
