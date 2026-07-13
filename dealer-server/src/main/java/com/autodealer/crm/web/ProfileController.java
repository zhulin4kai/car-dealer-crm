package com.autodealer.crm.web;
import com.autodealer.crm.dto.profile.ProfileDtos.*;
import com.autodealer.crm.result.R;
import com.autodealer.crm.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/profile")
public class ProfileController {
  private final ProfileService service;public ProfileController(ProfileService service){this.service=service;}
  @GetMapping public R<Profile> get(){return R.OK(service.getOwn());}
  @PutMapping public R<Profile> update(@Valid @RequestBody UpdateRequest request){return R.OK(service.updateOwn(request));}
}
