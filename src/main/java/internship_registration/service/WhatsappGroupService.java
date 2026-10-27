package internship_registration.service;

import internship_registration.dto.WhatsappGroupRequest;
import internship_registration.dto.WhatsappGroupResponse;
import internship_registration.entity.College;
import internship_registration.entity.InternshipBatch;
import internship_registration.entity.WhatsappGroup;
import internship_registration.exception.DuplicateResourceException;
import internship_registration.repository.CollegeRepository;
import internship_registration.repository.InternshipBatchRepository;
import internship_registration.repository.WhatsappGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

/** Admin CRUD for the WhatsApp group (invite link) of each college and batch. */
@Service
@RequiredArgsConstructor
public class WhatsappGroupService {

    private static final String INVITE_PREFIX = "https://chat.whatsapp.com/";

    private final WhatsappGroupRepository groupRepository;
    private final CollegeRepository collegeRepository;
    private final InternshipBatchRepository batchRepository;

    // ---------------------------------------------------------------- read
    public List<WhatsappGroupResponse> list(Integer collegeId, Integer batchId) {
        List<WhatsappGroup> groups = new ArrayList<>();
        if (collegeId != null && batchId != null) {
            Optional<WhatsappGroup> found = groupRepository.findByCollegeIdAndBatchId(collegeId, batchId);
            found.ifPresent(groups::add);
        } else if (collegeId != null) {
            groups.addAll(groupRepository.findByCollegeId(collegeId));
        } else if (batchId != null) {
            groups.addAll(groupRepository.findByBatchId(batchId));
        } else {
            groups.addAll(groupRepository.findAll());
        }
        return toResponses(groups);
    }

    public WhatsappGroupResponse get(Integer id) {
        return toResponses(List.of(find(id))).get(0);
    }

    // -------------------------------------------------------------- create
    @Transactional
    public WhatsappGroupResponse create(WhatsappGroupRequest request) {
        if (request.getCollegeId() == null) {
            throw new IllegalArgumentException("College ID is required");
        }
        if (request.getBatchId() == null) {
            throw new IllegalArgumentException("Batch ID is required");
        }
        String name = requireText(request.getGroupName(), "Group name");
        String url = validateUrl(request.getGroupUrl());

        requireCollege(request.getCollegeId());
        requireBatch(request.getBatchId());

        if (groupRepository.existsByCollegeIdAndBatchId(request.getCollegeId(), request.getBatchId())) {
            throw new DuplicateResourceException("A WhatsApp group already exists for this college and batch");
        }

        WhatsappGroup group = WhatsappGroup.builder()
                .collegeId(request.getCollegeId())
                .batchId(request.getBatchId())
                .groupName(name)
                .groupUrl(url)
                .isActive(request.getIsActive() == null || request.getIsActive())
                .build();

        return toResponses(List.of(groupRepository.save(group))).get(0);
    }

    // -------------------------------------------------------------- update
    @Transactional
    public WhatsappGroupResponse update(Integer id, WhatsappGroupRequest request) {
        WhatsappGroup group = find(id);

        Integer collegeId = request.getCollegeId() != null ? request.getCollegeId() : group.getCollegeId();
        Integer batchId = request.getBatchId() != null ? request.getBatchId() : group.getBatchId();

        if (!collegeId.equals(group.getCollegeId())) {
            requireCollege(collegeId);
        }
        if (!batchId.equals(group.getBatchId())) {
            requireBatch(batchId);
        }
        if (groupRepository.existsByCollegeIdAndBatchIdAndIdNot(collegeId, batchId, id)) {
            throw new DuplicateResourceException("A WhatsApp group already exists for this college and batch");
        }

        group.setCollegeId(collegeId);
        group.setBatchId(batchId);
        if (request.getGroupName() != null) group.setGroupName(requireText(request.getGroupName(), "Group name"));
        if (request.getGroupUrl() != null) group.setGroupUrl(validateUrl(request.getGroupUrl()));
        if (request.getIsActive() != null) group.setIsActive(request.getIsActive());

        return toResponses(List.of(groupRepository.save(group))).get(0);
    }

    // -------------------------------------------------------------- delete
    @Transactional
    public void delete(Integer id) {
        groupRepository.delete(find(id));
    }

    // ------------------------------------------------------------- helpers
    private WhatsappGroup find(Integer id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("WhatsApp group not found: " + id));
    }

    private void requireCollege(Integer collegeId) {
        if (!collegeRepository.existsById(collegeId)) {
            throw new IllegalArgumentException("College not found: " + collegeId);
        }
    }

    private void requireBatch(Integer batchId) {
        if (!batchRepository.existsById(batchId)) {
            throw new IllegalArgumentException("Batch not found: " + batchId);
        }
    }

    private String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required");
        }
        return value.trim();
    }

    private String validateUrl(String raw) {
        String url = requireText(raw, "Group link");
        boolean hasSpace = url.chars().anyMatch(Character::isWhitespace);
        if (!url.startsWith(INVITE_PREFIX) || url.length() <= INVITE_PREFIX.length()
                || url.length() > 1000 || hasSpace) {
            throw new IllegalArgumentException(
                    "Group link must be a WhatsApp invite link like " + INVITE_PREFIX + "XXXXXXXX");
        }
        return url;
    }

    // One query per table for the whole list, so the college and batch names don't cause N+1 lookups
    private List<WhatsappGroupResponse> toResponses(List<WhatsappGroup> groups) {
        Set<Integer> collegeIds = new HashSet<>();
        Set<Integer> batchIds = new HashSet<>();
        for (WhatsappGroup g : groups) {
            collegeIds.add(g.getCollegeId());
            batchIds.add(g.getBatchId());
        }

        Map<Integer, College> colleges = new HashMap<>();
        for (College c : collegeRepository.findAllById(collegeIds)) {
            colleges.put(c.getId(), c);
        }
        Map<Integer, InternshipBatch> batches = new HashMap<>();
        for (InternshipBatch b : batchRepository.findAllById(batchIds)) {
            batches.put(b.getId(), b);
        }

        List<WhatsappGroupResponse> result = new ArrayList<>();
        for (WhatsappGroup g : groups) {
            College college = colleges.get(g.getCollegeId());
            InternshipBatch batch = batches.get(g.getBatchId());
            result.add(WhatsappGroupResponse.builder()
                    .id(g.getId())
                    .collegeId(g.getCollegeId())
                    .collegeName(college != null ? college.getName() : null)
                    .collegeCode(college != null ? college.getCode() : null)
                    .batchId(g.getBatchId())
                    .batchName(batch != null ? batch.getBatchName() : null)
                    .courseName(batch != null && batch.getCourse() != null ? batch.getCourse().getName() : null)
                    .groupName(g.getGroupName())
                    .groupUrl(g.getGroupUrl())
                    .isActive(g.getIsActive())
                    .createdAt(g.getCreatedAt())
                    .updatedAt(g.getUpdatedAt())
                    .build());
        }
        return result;
    }
}