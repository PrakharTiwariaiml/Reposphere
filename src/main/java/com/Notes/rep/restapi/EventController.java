package com.Notes.rep.restapi;

import com.Notes.rep.Repository.EventRepository;
import com.Notes.rep.config.UserRepository;
import com.Notes.rep.entity.CalendarEvent;
import com.Notes.rep.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
@RestController
@RequestMapping("/api/events")
// Note: added allowCredentials
public class EventController {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    // Helper method to get the user from any OAuth2 or Local Session
    private User getAuthenticatedUser(Authentication authentication) {
        String email;
        if (authentication.getPrincipal() instanceof OAuth2User oAuth2User) {
            email = oAuth2User.getAttribute("email");
        } else {
            email = authentication.getName();
        }

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    @PostMapping("/add")
    public ResponseEntity<CalendarEvent> addEvent(@RequestBody CalendarEvent event, Authentication authentication) {
        // Fix: Use the email-based helper instead of findByUsername
        User currentUser = getAuthenticatedUser(authentication);

        event.setUser(currentUser);
        CalendarEvent savedEvent = eventRepository.save(event);
        return ResponseEntity.ok(savedEvent);
    }

    @GetMapping("/my-events")
    public ResponseEntity<List<CalendarEvent>> getUserEvents(Authentication authentication) {
        User currentUser = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(eventRepository.findByUserId(currentUser.getId()));
    }
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteEvent(@PathVariable Long id, Authentication authentication) {
        // 1. Get the current logged-in user
        User currentUser = getAuthenticatedUser(authentication);

        // 2. Find the event by its OWN ID (not the user's ID)
        CalendarEvent event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + id));

        // 3. Security Check: Compare the Owner's ID with the Current User's ID
        if (!event.getUser().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(403).body("Error: You don't have permission to delete this event!");
        }

        // 4. Perform the deletion using the ID
        eventRepository.deleteById(id);

        return ResponseEntity.ok("Event deleted successfully!");
    }
}