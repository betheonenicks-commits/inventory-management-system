package com.iams.usr.application;

import com.iams.usr.domain.AppUser;
import com.iams.usr.domain.Role;
import java.util.List;

/** An AppUser plus its currently-assigned roles (US-USR-07: flat, could be more than one). */
public record UserWithRoles(AppUser user, List<Role> roles) {
}
