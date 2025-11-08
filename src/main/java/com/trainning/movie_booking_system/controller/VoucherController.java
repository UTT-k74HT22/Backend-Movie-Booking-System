package com.trainning.movie_booking_system.controller;

import com.trainning.movie_booking_system.dto.request.Voucher.CreateVoucherRequest;
import com.trainning.movie_booking_system.dto.request.Voucher.UpdateVoucherRequest;
import com.trainning.movie_booking_system.dto.request.Voucher.ValidateVoucherRequest;
import com.trainning.movie_booking_system.dto.response.Voucher.VoucherResponse;
import com.trainning.movie_booking_system.dto.response.Voucher.VoucherUsageResponse;
import com.trainning.movie_booking_system.dto.response.Voucher.VoucherValidationResult;
import com.trainning.movie_booking_system.security.CustomAccountDetails;
import com.trainning.movie_booking_system.service.IVoucherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Voucher operations
 * Handles voucher validation, usage history, and admin management
 */
@RestController
@RequestMapping("/api/v1/vouchers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Voucher", description = "Voucher management APIs")
public class VoucherController {

    private final IVoucherService voucherService;

    /**
     * ============================================
     * USER ENDPOINTS
     * ============================================
     */

    /**
     * Validate a voucher for a booking
     * POST /api/v1/vouchers/validate
     * 
     * TODO: Implement validation endpoint
     * - Extract user ID from authentication principal
     * - Call voucherService.validateVoucher()
     * - Return validation result with discount calculation
     */
    @PostMapping("/validate")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Validate voucher",
        description = "Validate if a voucher can be applied to a booking and calculate discount"
    )
    public ResponseEntity<VoucherValidationResult> validateVoucher(
            @Valid @RequestBody ValidateVoucherRequest request,
            @AuthenticationPrincipal CustomAccountDetails accountDetails
    ) {
        log.info("REST request to validate voucher: {}", request.getVoucherCode());

        // TODO: Get user ID from CustomAccountDetails
        // Long userId = accountDetails.getId();

        // TODO: Call service to validate voucher
        // VoucherValidationResult result = voucherService.validateVoucher(request, userId);

        // TODO: Return result
        // return ResponseEntity.ok(result);

        throw new UnsupportedOperationException("TODO: Implement validateVoucher endpoint");
    }

    /**
     * Get all public vouchers (available vouchers)
     * GET /api/v1/vouchers/public
     * 
     * TODO: Implement public vouchers list endpoint
     * - Parse pagination parameters
     * - Call voucherService.getPublicVouchers()
     * - Return page of public vouchers
     */
    @GetMapping("/public")
    @Operation(
        summary = "Get public vouchers",
        description = "Get all active public vouchers that can be used"
    )
    public ResponseEntity<Page<VoucherResponse>> getPublicVouchers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "validUntil") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDir
    ) {
        log.info("REST request to get public vouchers - page: {}, size: {}", page, size);

        // TODO: Create pageable with sorting
        // Sort sort = sortDir.equalsIgnoreCase("DESC") 
        //     ? Sort.by(sortBy).descending() 
        //     : Sort.by(sortBy).ascending();
        // Pageable pageable = PageRequest.of(page, size, sort);

        // TODO: Call service to get public vouchers
        // Page<VoucherResponse> vouchers = voucherService.getPublicVouchers(pageable);

        // TODO: Return page of vouchers
        // return ResponseEntity.ok(vouchers);

        throw new UnsupportedOperationException("TODO: Implement getPublicVouchers endpoint");
    }

    /**
     * Get user's voucher usage history
     * GET /api/v1/vouchers/my-usage
     * 
     * TODO: Implement user voucher usage history endpoint
     * - Extract user ID from authentication principal
     * - Parse pagination parameters
     * - Call voucherService.getUserVoucherUsageHistory()
     * - Return page of usage history
     */
    @GetMapping("/my-usage")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Get my voucher usage history",
        description = "Get current user's voucher usage history"
    )
    public ResponseEntity<Page<VoucherUsageResponse>> getMyVoucherUsageHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomAccountDetails accountDetails
    ) {
        log.info("REST request to get voucher usage history for user");

        // TODO: Get user ID from CustomAccountDetails
        // Long userId = accountDetails.getId();

        // TODO: Create pageable with sorting by usedAt descending
        // Pageable pageable = PageRequest.of(page, size, Sort.by("usedAt").descending());

        // TODO: Call service to get usage history
        // Page<VoucherUsageResponse> usageHistory = voucherService.getUserVoucherUsageHistory(userId, pageable);

        // TODO: Return page of usage history
        // return ResponseEntity.ok(usageHistory);

        throw new UnsupportedOperationException("TODO: Implement getMyVoucherUsageHistory endpoint");
    }

    /**
     * ============================================
     * ADMIN ENDPOINTS
     * ============================================
     */

    /**
     * Create a new voucher (ADMIN only)
     * POST /api/v1/vouchers
     * 
     * TODO: Implement create voucher endpoint
     * - Validate request body
     * - Call voucherService.createVoucher()
     * - Return created voucher
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Create voucher (ADMIN)",
        description = "Create a new voucher (Admin only)"
    )
    public ResponseEntity<VoucherResponse> createVoucher(
            @Valid @RequestBody CreateVoucherRequest request
    ) {
        log.info("REST request to create voucher: {}", request.getCode());

        // TODO: Call service to create voucher
        // VoucherResponse voucher = voucherService.createVoucher(request);

        // TODO: Return created voucher
        // return ResponseEntity.ok(voucher);

        throw new UnsupportedOperationException("TODO: Implement createVoucher endpoint");
    }

    /**
     * Update an existing voucher (ADMIN only)
     * PUT /api/v1/vouchers/{id}
     * 
     * TODO: Implement update voucher endpoint
     * - Extract voucher ID from path
     * - Validate request body
     * - Call voucherService.updateVoucher()
     * - Return updated voucher
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Update voucher (ADMIN)",
        description = "Update an existing voucher (Admin only)"
    )
    public ResponseEntity<VoucherResponse> updateVoucher(
            @PathVariable Long id,
            @Valid @RequestBody UpdateVoucherRequest request
    ) {
        log.info("REST request to update voucher: {}", id);

        // TODO: Call service to update voucher
        // VoucherResponse voucher = voucherService.updateVoucher(id, request);

        // TODO: Return updated voucher
        // return ResponseEntity.ok(voucher);

        throw new UnsupportedOperationException("TODO: Implement updateVoucher endpoint");
    }

    /**
     * Delete a voucher (ADMIN only)
     * DELETE /api/v1/vouchers/{id}
     * 
     * TODO: Implement delete voucher endpoint
     * - Extract voucher ID from path
     * - Call voucherService.deleteVoucher()
     * - Return 204 No Content
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Delete voucher (ADMIN)",
        description = "Soft delete a voucher by setting status to INACTIVE (Admin only)"
    )
    public ResponseEntity<Void> deleteVoucher(@PathVariable Long id) {
        log.info("REST request to delete voucher: {}", id);

        // TODO: Call service to delete voucher
        // voucherService.deleteVoucher(id);

        // TODO: Return 204 No Content
        // return ResponseEntity.noContent().build();

        throw new UnsupportedOperationException("TODO: Implement deleteVoucher endpoint");
    }

    /**
     * Get voucher by ID (ADMIN only)
     * GET /api/v1/vouchers/{id}
     * 
     * TODO: Implement get voucher by ID endpoint
     * - Extract voucher ID from path
     * - Call voucherService.getVoucherById()
     * - Return voucher details
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Get voucher by ID (ADMIN)",
        description = "Get detailed information of a voucher (Admin only)"
    )
    public ResponseEntity<VoucherResponse> getVoucherById(@PathVariable Long id) {
        log.info("REST request to get voucher: {}", id);

        // TODO: Call service to get voucher
        // VoucherResponse voucher = voucherService.getVoucherById(id);

        // TODO: Return voucher
        // return ResponseEntity.ok(voucher);

        throw new UnsupportedOperationException("TODO: Implement getVoucherById endpoint");
    }

    /**
     * Get all vouchers (ADMIN only)
     * GET /api/v1/vouchers
     * 
     * TODO: Implement get all vouchers endpoint
     * - Parse pagination parameters
     * - Call voucherService.getAllVouchers()
     * - Return page of all vouchers
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Get all vouchers (ADMIN)",
        description = "Get all vouchers with pagination (Admin only)"
    )
    public ResponseEntity<Page<VoucherResponse>> getAllVouchers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir
    ) {
        log.info("REST request to get all vouchers - page: {}, size: {}", page, size);

        // TODO: Create pageable with sorting
        // Sort sort = sortDir.equalsIgnoreCase("DESC") 
        //     ? Sort.by(sortBy).descending() 
        //     : Sort.by(sortBy).ascending();
        // Pageable pageable = PageRequest.of(page, size, sort);

        // TODO: Call service to get all vouchers
        // Page<VoucherResponse> vouchers = voucherService.getAllVouchers(pageable);

        // TODO: Return page of vouchers
        // return ResponseEntity.ok(vouchers);

        throw new UnsupportedOperationException("TODO: Implement getAllVouchers endpoint");
    }
}
