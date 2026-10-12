package com.studentmanager.user;

import com.studentmanager.common.ApiResponse;
import com.studentmanager.common.exception.ApiExceptions.ResourceNotFoundException;
import com.studentmanager.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public record MeResponse(UUID id, String name, String email) {}

    /**
     * Returns the profile of whoever's access token is on the request -
     * never a client-supplied id. Open this from two different devices
     * logged into the same account and you get back the same user.id both
     * times; that equality is the actual, verifiable test for "cross-device
     * sign-in works."
     */
    @GetMapping("/me")
    public ApiResponse<MeResponse> me(@AuthenticationPrincipal AuthenticatedUser principal) {
        User user = userRepository.findById(principal.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        return ApiResponse.of(new MeResponse(user.getId(), user.getName(), user.getEmail()));
    }
}
