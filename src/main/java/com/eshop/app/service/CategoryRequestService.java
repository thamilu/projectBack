package com.eshop.app.service;

import com.eshop.app.dto.response.CategoryRequestResponse;
import com.eshop.app.dto.request.ReviewRequest;
import com.eshop.app.entity.Category;
import com.eshop.app.entity.CategoryRequest;
import com.eshop.app.entity.RequestStatus;
import com.eshop.app.entity.User;
import com.eshop.app.exception.ResourceNotFoundException;
import com.eshop.app.repository.CategoryRepository;
import com.eshop.app.repository.CategoryRequestRepository;
import com.eshop.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryRequestService {
    private final CategoryRequestRepository requestRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public CategoryRequest createRequest(com.eshop.app.dto.request.CategoryRequest dto, Long sellerId) {
        if (categoryRepository.existsByNameIgnoreCase(dto.getCategoryName())) {
            throw new RuntimeException("Category already exists: " + dto.getCategoryName());
        }
        if (requestRepository.existsByCategoryNameIgnoreCase(dto.getCategoryName())) {
            throw new RuntimeException("Request for this category is already pending");
        }
        User seller = userRepository.findById(sellerId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", sellerId));
        CategoryRequest request = new CategoryRequest();
        request.setCategoryName(dto.getCategoryName());
        request.setDescription(dto.getDescription());
        request.setReason(dto.getReason());
        request.setSeller(seller);
        request.setStatus(RequestStatus.PENDING);
        return requestRepository.save(request);
    }

    public List<CategoryRequestResponse> getSellerRequests(Long sellerId) {
        return requestRepository.findBySellerId(sellerId)
            .stream()
            .map(CategoryRequestResponse::new)
            .toList();
    }

    public List<CategoryRequestResponse> getPendingRequests() {
        return requestRepository.findByStatus(RequestStatus.PENDING)
            .stream()
            .map(CategoryRequestResponse::new)
            .toList();
    }

    @Transactional
    public CategoryRequest reviewRequest(Long requestId, ReviewRequest dto, Long adminId) {
        CategoryRequest request = requestRepository.findById(requestId)
            .orElseThrow(() -> new ResourceNotFoundException("CategoryRequest", "id", requestId));
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new RuntimeException("Request already reviewed");
        }
        User admin = userRepository.findById(adminId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", adminId));
        boolean approved = dto.getApproved();
        if (approved) {
            // Create the category
            Category category = new Category();
            category.setName(request.getCategoryName());
            category.setDescription(request.getDescription());
            categoryRepository.save(category);
            request.setStatus(RequestStatus.APPROVED);
        } else {
            request.setStatus(RequestStatus.REJECTED);
        }
        request.setAdminRemarks(dto.getRemarks());
        request.setReviewedBy(admin);
        request.setReviewedAt(LocalDateTime.now());
        CategoryRequest savedRequest = requestRepository.save(request);
        // Send email notification to seller
        sendCategoryRequestEmail(request, approved);
        return savedRequest;
    }

    private void sendCategoryRequestEmail(CategoryRequest request, boolean approved) {
        String to = request.getSeller().getEmail();
        String subject = approved ? "Category Request Approved" : "Category Request Rejected";
        String text = approved ?
            "Your category request for '" + request.getCategoryName() + "' has been approved.\nRemarks: " + request.getAdminRemarks() :
            "Your category request for '" + request.getCategoryName() + "' has been rejected.\nRemarks: " + request.getAdminRemarks();
        try {
            emailService.sendEmail(to, subject, text);
        } catch (Exception e) {
            // Log error or handle as needed
        }
    }
}
