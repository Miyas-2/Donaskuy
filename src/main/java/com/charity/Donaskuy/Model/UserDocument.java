package com.charity.Donaskuy.Model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
@Entity
@Table(name = "user_documents")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nik;
    private String nama;
    private String tempatLahir;
    private String tanggalLahir;

    public enum JenisKelamin { LAKI_LAKI, PEREMPUAN }
    @Enumerated(EnumType.STRING)
    private JenisKelamin jenisKelamin;

    private String alamat;
    private String rtRw;
    private String kelDesa;
    private String kecamatan;
    private String agama;
    private String statusPerkawinan;
    private String pekerjaan;
    private String kewarganegaraan;
    private String golDarah;

    private String fotoKtp;
    private String fotoSelfie;

    @Enumerated(EnumType.STRING)
    private DocumentStatus status = DocumentStatus.PENDING;

    private LocalDateTime uploadedAt = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    public enum DocumentStatus {
        PENDING, VERIFIED, REJECTED
    }
}