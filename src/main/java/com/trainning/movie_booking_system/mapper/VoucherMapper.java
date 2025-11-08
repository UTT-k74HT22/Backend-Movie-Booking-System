package com.trainning.movie_booking_system.mapper;

import com.trainning.movie_booking_system.dto.request.Voucher.CreateVoucherRequest;
import com.trainning.movie_booking_system.dto.response.Voucher.VoucherResponse;
import com.trainning.movie_booking_system.dto.response.Voucher.VoucherUsageResponse;
import com.trainning.movie_booking_system.entity.Voucher;
import com.trainning.movie_booking_system.entity.VoucherUsage;
import org.mapstruct.*;

import java.util.List;

/**
 * MapStruct mapper for Voucher and VoucherUsage entities
 * Uses manual conversion methods for JSON fields
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface VoucherMapper {

    /**
     * Map CreateVoucherRequest to Voucher entity
     * JSON fields will be handled manually in service
     */
    @Mapping(target = "currentUsageCount", constant = "0")
    @Mapping(target = "applicableMovieIds", ignore = true) // Handle manually in service
    @Mapping(target = "applicableTheaterIds", ignore = true) // Handle manually in service
    @Mapping(target = "applicableDaysOfWeek", ignore = true) // Handle manually in service
    @Mapping(target = "applicableTimeSlots", ignore = true) // Handle manually in service
    Voucher toEntity(CreateVoucherRequest request);

    /**
     * Map Voucher entity to VoucherResponse
     * JSON fields will be handled manually in service
     */
    @Mapping(target = "applicableMovieIds", ignore = true) // Handle manually in service
    @Mapping(target = "applicableTheaterIds", ignore = true) // Handle manually in service
    @Mapping(target = "applicableDaysOfWeek", ignore = true) // Handle manually in service
    @Mapping(target = "applicableTimeSlots", ignore = true) // Handle manually in service
    @Mapping(target = "canUse", ignore = true) // Set manually in service
    @Mapping(target = "userRemainingUsage", ignore = true) // Set manually in service
    VoucherResponse toResponse(Voucher voucher);

    /**
     * Map list of Voucher entities to list of VoucherResponse
     */
    List<VoucherResponse> toResponseList(List<Voucher> vouchers);

    /**
     * Map VoucherUsage entity to VoucherUsageResponse
     */
    @Mapping(source = "voucher.id", target = "voucherId")
    @Mapping(source = "voucher.code", target = "voucherCode")
    @Mapping(source = "voucher.name", target = "voucherName")
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.email", target = "userEmail")
    @Mapping(source = "booking.id", target = "bookingId")
    @Mapping(source = "booking.status", target = "bookingStatus")
    VoucherUsageResponse toUsageResponse(VoucherUsage voucherUsage);

    /**
     * Map list of VoucherUsage entities to list of VoucherUsageResponse
     */
    List<VoucherUsageResponse> toUsageResponseList(List<VoucherUsage> voucherUsages);
}
