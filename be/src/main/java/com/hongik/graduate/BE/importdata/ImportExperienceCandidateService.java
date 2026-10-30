package com.hongik.graduate.BE.importdata;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.hongik.graduate.BE.common.exception.BusinessException;
import com.hongik.graduate.BE.common.exception.ErrorCode;
import com.hongik.graduate.ai.experience.ExperienceSummary.SourceRef;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Evidence-only extraction. Not semantic experience detection or OCR. */
@Service
public class ImportExperienceCandidateService {
    private static final int MAX_PAGES = 60;
    private static final int MAX_CHARS = 60000;
    private static final int MAX_CANDIDATES = 100;
    private final ImportFileService files;

    public ImportExperienceCandidateService(ImportFileService files) {
        this.files = files;
    }

    public record Candidate(UUID sourceId, String sourceType, String location,
            String quote, String status) { }

    public record CandidateResult(UUID sourceId, String sourceType, String status,
            List<Candidate> candidates) {
        public CandidateResult {
            candidates = List.copyOf(candidates);
        }
    }

    @Transactional(readOnly = true)
    public CandidateResult extract(UUID memberId, UUID sourceId) {
        ImportedSource source = files.get(memberId, sourceId);
        List<Candidate> candidates = new ArrayList<>();
        if ("URL".equals(source.sourceType)) {
            addCandidates(source, "url", source.collectedText, candidates);
        } else if ("PDF".equals(source.sourceType)) {
            byte[] bytes = files.download(memberId, sourceId);
            try (var pdf = Loader.loadPDF(bytes)) {
                if (pdf.isEncrypted() || pdf.getNumberOfPages() > MAX_PAGES) {
                    throw new BusinessException(ErrorCode.INVALID_REQUEST);
                }
                PDFTextStripper stripper = new PDFTextStripper();
                int remaining = MAX_CHARS;
                for (int page = 1; page <= pdf.getNumberOfPages()
                        && remaining > 0 && candidates.size() < MAX_CANDIDATES; page++) {
                    stripper.setStartPage(page);
                    stripper.setEndPage(page);
                    String text = stripper.getText(pdf);
                    if (text.length() > remaining) {
                        text = text.substring(0, remaining);
                    }
                    remaining -= text.length();
                    addCandidates(source, "page:" + page, text, candidates);
                }
            } catch (IOException | IllegalArgumentException e) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST);
            }
        } else {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        return new CandidateResult(sourceId, source.sourceType,
                candidates.isEmpty() ? "NO_TEXT" : "REVIEW_REQUIRED", candidates);
    }

    private static void addCandidates(ImportedSource source, String locationPrefix,
            String text, List<Candidate> candidates) {
        if (text == null || text.isBlank()) {
            return;
        }
        String normalized = text.replace("\r", "").strip();
        if (normalized.length() > MAX_CHARS) {
            normalized = normalized.substring(0, MAX_CHARS);
        }
        String[] chunks = normalized.split("(?:\\n\\s*\\n)+|(?<=\\.)\\s*\\n");
        for (int i = 0; i < chunks.length && candidates.size() < MAX_CANDIDATES; i++) {
            String quote = chunks[i].strip();
            if (quote.isEmpty()) {
                continue;
            }
            // An excerpt must be verbatim, not a fabricated STAR conclusion.
            if (quote.length() > 1000) {
                quote = quote.substring(0, 1000);
            }
            candidates.add(new Candidate(source.id, source.sourceType,
                    locationPrefix + ":paragraph:" + (i + 1), quote, "REVIEW_REQUIRED"));
        }
    }

    public void verifyEvidence(UUID memberId, SourceRef evidence) {
        CandidateResult result = extract(memberId, evidence.sourceId());
        boolean found = result.candidates().stream().anyMatch(item ->
                item.location().equals(evidence.location())
                        && item.quote().equals(evidence.quote()));
        if (!found) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }
}
