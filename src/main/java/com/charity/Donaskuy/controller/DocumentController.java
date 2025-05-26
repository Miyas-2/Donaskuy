package com.charity.Donaskuy.controller;

import java.io.File;
import java.io.IOException;

import com.charity.Donaskuy.Model.User;
import com.charity.Donaskuy.Model.UserDocument;
import com.charity.Donaskuy.repository.UserDocumentRepository;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequiredArgsConstructor
public class DocumentController {

    private final UserDocumentRepository documentRepository;

    @GetMapping("/dashboard/upload")
    public String showUploadForm(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        var documents = documentRepository.findAllByUser(user);
        model.addAttribute("documents", documents);

        UserDocument lastDoc = documents.isEmpty() ? null : documents.get(documents.size() - 1);
        boolean canEdit = lastDoc != null && lastDoc.getStatus() == UserDocument.DocumentStatus.REJECTED;
        boolean canUpload = lastDoc == null;

        model.addAttribute("canEdit", canEdit);
        model.addAttribute("canUpload", canUpload);
        model.addAttribute("document", canEdit ? lastDoc : new UserDocument());

        return "upload_document";
    }

    @PostMapping("/dashboard/upload")
    public String uploadDocument(
            @RequestParam("nik") String nik,
            @RequestParam("nama") String nama,
            @RequestParam("tempatLahir") String tempatLahir,
            @RequestParam("tanggalLahir") String tanggalLahir,
            @RequestParam("jenisKelamin") UserDocument.JenisKelamin jenisKelamin,
            @RequestParam("alamat") String alamat,
            @RequestParam("rtRw") String rtRw,
            @RequestParam("kelDesa") String kelDesa,
            @RequestParam("kecamatan") String kecamatan,
            @RequestParam("agama") String agama,
            @RequestParam("statusPerkawinan") String statusPerkawinan,
            @RequestParam("pekerjaan") String pekerjaan,
            @RequestParam("kewarganegaraan") String kewarganegaraan,
            @RequestParam("golDarah") String golDarah,
            @RequestParam("fotoKtp") MultipartFile fotoKtp,
            @RequestParam("fotoSelfie") MultipartFile fotoSelfie,
            HttpSession session
    ) throws IOException {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        var documents = documentRepository.findAllByUser(user);
        UserDocument lastDoc = documents.isEmpty() ? null : documents.get(documents.size() - 1);
        if (lastDoc != null && lastDoc.getStatus() != UserDocument.DocumentStatus.REJECTED) {
            // Tidak boleh upload jika status terakhir bukan REJECTED
            return "redirect:/dashboard/upload";
        }

        String uploadDir = System.getProperty("user.dir") + "/uploads/";
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String fotoKtpName = System.currentTimeMillis() + "_ktp_" + fotoKtp.getOriginalFilename();
        fotoKtp.transferTo(new File(uploadDir + fotoKtpName));

        String fotoSelfieName = System.currentTimeMillis() + "_selfie_" + fotoSelfie.getOriginalFilename();
        fotoSelfie.transferTo(new File(uploadDir + fotoSelfieName));

        UserDocument doc = new UserDocument();
        doc.setNik(nik);
        doc.setNama(nama);
        doc.setTempatLahir(tempatLahir);
        doc.setTanggalLahir(tanggalLahir);
        doc.setJenisKelamin(jenisKelamin);
        doc.setAlamat(alamat);
        doc.setRtRw(rtRw);
        doc.setKelDesa(kelDesa);
        doc.setKecamatan(kecamatan);
        doc.setAgama(agama);
        doc.setStatusPerkawinan(statusPerkawinan);
        doc.setPekerjaan(pekerjaan);
        doc.setKewarganegaraan(kewarganegaraan);
        doc.setGolDarah(golDarah);
        doc.setFotoKtp(fotoKtpName);
        doc.setFotoSelfie(fotoSelfieName);
        doc.setUser(user);

        documentRepository.save(doc);
        return "redirect:/dashboard/upload";
    }

    @PostMapping("/dashboard/upload/edit")
    public String editDocument(
            @RequestParam("id") Long id,
            @RequestParam("nik") String nik,
            @RequestParam("nama") String nama,
            @RequestParam("tempatLahir") String tempatLahir,
            @RequestParam("tanggalLahir") String tanggalLahir,
            @RequestParam("jenisKelamin") UserDocument.JenisKelamin jenisKelamin,
            @RequestParam("alamat") String alamat,
            @RequestParam("rtRw") String rtRw,
            @RequestParam("kelDesa") String kelDesa,
            @RequestParam("kecamatan") String kecamatan,
            @RequestParam("agama") String agama,
            @RequestParam("statusPerkawinan") String statusPerkawinan,
            @RequestParam("pekerjaan") String pekerjaan,
            @RequestParam("kewarganegaraan") String kewarganegaraan,
            @RequestParam("golDarah") String golDarah,
            @RequestParam(value = "fotoKtp", required = false) MultipartFile fotoKtp,
            @RequestParam(value = "fotoSelfie", required = false) MultipartFile fotoSelfie,
            HttpSession session
    ) throws IOException {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        UserDocument doc = documentRepository.findById(id).orElse(null);
        if (doc == null || doc.getStatus() != UserDocument.DocumentStatus.REJECTED) {
            return "redirect:/dashboard/upload";
        }

        doc.setNik(nik);
        doc.setNama(nama);
        doc.setTempatLahir(tempatLahir);
        doc.setTanggalLahir(tanggalLahir);
        doc.setJenisKelamin(jenisKelamin);
        doc.setAlamat(alamat);
        doc.setRtRw(rtRw);
        doc.setKelDesa(kelDesa);
        doc.setKecamatan(kecamatan);
        doc.setAgama(agama);
        doc.setStatusPerkawinan(statusPerkawinan);
        doc.setPekerjaan(pekerjaan);
        doc.setKewarganegaraan(kewarganegaraan);
        doc.setGolDarah(golDarah);

        String uploadDir = System.getProperty("user.dir") + "/uploads/";
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        if (fotoKtp != null && !fotoKtp.isEmpty()) {
            String fotoKtpName = System.currentTimeMillis() + "_ktp_" + fotoKtp.getOriginalFilename();
            fotoKtp.transferTo(new File(uploadDir + fotoKtpName));
            doc.setFotoKtp(fotoKtpName);
        }
        if (fotoSelfie != null && !fotoSelfie.isEmpty()) {
            String fotoSelfieName = System.currentTimeMillis() + "_selfie_" + fotoSelfie.getOriginalFilename();
            fotoSelfie.transferTo(new File(uploadDir + fotoSelfieName));
            doc.setFotoSelfie(fotoSelfieName);
        }

        // Set status kembali ke PENDING setelah diedit
        doc.setStatus(UserDocument.DocumentStatus.PENDING);
        doc.setUploadedAt(java.time.LocalDateTime.now());

        documentRepository.save(doc);
        return "redirect:/dashboard/upload";
    }
}
