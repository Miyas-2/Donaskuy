package com.charity.Donaskuy.Model;

import java.time.LocalDate;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "donation_programs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DonationProgram {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;
    private Double targetAmount;

    private LocalDate startDate;
    private LocalDate endDate;

    private String photo;

    @Enumerated(EnumType.STRING)
    private ProgramStatus status = ProgramStatus.PENDING;

    @Column(name = "collected_amount")
    private Double collectedAmount = 0.0;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "donationProgram")
    private List<Donation> donations;

    public enum ProgramStatus {
        PENDING, APPROVED, REJECTED
    }

    // ...existing code...
    public Double getTotalDonasi() {
        if (donations == null) {
            return 0.0;
        }
        return donations.stream()
                .filter(d -> d.getPaymentStatus() == Donation.PaymentStatus.COMPLETED)
                .mapToDouble(Donation::getAmount)
                .sum();
    }
}
