package internship_registration.controller;

import internship_registration.dto.MessageResponse;
import internship_registration.dto.WhatsappGroupRequest;
import internship_registration.dto.WhatsappGroupResponse;
import internship_registration.service.WhatsappGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/whatsapp-groups")
@RequiredArgsConstructor
public class WhatsappGroupController {

    private final WhatsappGroupService whatsappGroupService;

    @GetMapping
    public ResponseEntity<List<WhatsappGroupResponse>> list(
            @RequestParam(required = false) Integer collegeId,
            @RequestParam(required = false) Integer batchId) {
        return ResponseEntity.ok(whatsappGroupService.list(collegeId, batchId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WhatsappGroupResponse> get(@PathVariable Integer id) {
        return ResponseEntity.ok(whatsappGroupService.get(id));
    }

    @PostMapping
    public ResponseEntity<WhatsappGroupResponse> create(@RequestBody WhatsappGroupRequest request) {
        return ResponseEntity.ok(whatsappGroupService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WhatsappGroupResponse> update(@PathVariable Integer id,
                                                        @RequestBody WhatsappGroupRequest request) {
        return ResponseEntity.ok(whatsappGroupService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> delete(@PathVariable Integer id) {
        whatsappGroupService.delete(id);
        return ResponseEntity.ok(new MessageResponse("WhatsApp group deleted successfully"));
    }
}