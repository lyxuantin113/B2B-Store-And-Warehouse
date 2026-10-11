package com.b2b.warehouse.partner.entity;

import com.b2b.warehouse.auth.entity.UserEntity;
import com.b2b.warehouse.common.entity.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "b2b_partner_profiles")
public class B2bPartnerProfileEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserEntity user;

    @Column(name = "company_name", nullable = false, length = 200)
    private String companyName;

    @Column(name = "tax_code", nullable = false, unique = true, length = 30)
    private String taxCode;

    @Column(name = "business_type", length = 80)
    private String businessType;

    @Column(name = "representative_name", nullable = false, length = 120)
    private String representativeName;

    @Column(name = "contact_person_name", nullable = false, length = 120)
    private String contactPersonName;

    @Column(name = "contact_phone", nullable = false, length = 25)
    private String contactPhone;

    @Column(name = "business_license_url", nullable = false, length = 500)
    private String businessLicenseUrl;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "address_line", nullable = false, length = 255)
    private String addressLine;

    @Column(name = "address_detail", length = 255)
    private String addressDetail;

    @Column(name = "shipping_postal_code", length = 20)
    private String shippingPostalCode;

    @Column(name = "shipping_address_line", length = 255)
    private String shippingAddressLine;

    @Column(name = "shipping_address_detail", length = 255)
    private String shippingAddressDetail;

    @Column(name = "approval_notice")
    private String approvalNotice;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "partner_confirmed_at")
    private Instant partnerConfirmedAt;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Builder.Default
    @Column(name = "reapply_count", nullable = false)
    private Integer reapplyCount = 0;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
