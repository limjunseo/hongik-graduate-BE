package com.hongik.graduate.BE.importdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.util.UUID;

import com.hongik.graduate.BE.common.exception.BusinessException;
import com.hongik.graduate.BE.common.exception.ErrorCode;
import com.hongik.graduate.ai.experience.ExperienceSummary.SourceRef;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

class ImportExperienceCandidateServiceTest {
    private static final UUID OWNER = UUID.randomUUID();
    private static final UUID OTHER = UUID.randomUUID();

    @Test
    void pdfExtractsVerbatimPageCandidateWithoutGeneratingStarFacts() throws Exception {
        byte[] pdfBytes = pdf("Resolved an application performance issue.");
        ImportedSource pdf = source("PDF");
        ImportFileService files = mock(ImportFileService.class);
        when(files.get(OWNER, pdf.id)).thenReturn(pdf);
        when(files.download(OWNER, pdf.id)).thenReturn(pdfBytes);
        when(files.get(OTHER, pdf.id))
                .thenThrow(new BusinessException(ErrorCode.COVER_LETTER_NOT_FOUND));
        ImportExperienceCandidateService service = new ImportExperienceCandidateService(files);

        var result = service.extract(OWNER, pdf.id);
        assertThat(result.status()).isEqualTo("REVIEW_REQUIRED");
        assertThat(result.candidates()).hasSize(1);
        var first = result.candidates().get(0);
        assertThat(first.location()).isEqualTo("page:1:paragraph:1");
        assertThat(first.quote()).contains("Resolved an application");
        assertThat(first.status()).isEqualTo("REVIEW_REQUIRED");
        service.verifyEvidence(OWNER,
                new SourceRef(pdf.id, first.location(), first.quote()));
        assertThatThrownBy(() -> service.verifyEvidence(OWNER,
                new SourceRef(pdf.id, first.location(), "fabricated statistic")))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.extract(OTHER, pdf.id))
                .isInstanceOf(BusinessException.class);
        verify(files, never()).download(OTHER, pdf.id);
    }

    @Test
    void scannedOrBlankPdfNeedsOcrButKeepsOriginalAvailable() throws Exception {
        byte[] blankPdf = pdf(null);
        ImportedSource pdf = source("PDF");
        ImportFileService files = mock(ImportFileService.class);
        when(files.get(OWNER, pdf.id)).thenReturn(pdf);
        when(files.download(OWNER, pdf.id)).thenReturn(blankPdf);
        var result = new ImportExperienceCandidateService(files).extract(OWNER, pdf.id);
        assertThat(result.status()).isEqualTo("NO_TEXT");
        assertThat(result.candidates()).isEmpty();
        verify(files, times(1)).download(OWNER, pdf.id);
    }

    @Test
    void invalidPdfIsRejectedWithoutDeletingOriginal() {
        ImportedSource pdf = source("PDF");
        ImportFileService files = mock(ImportFileService.class);
        when(files.get(OWNER, pdf.id)).thenReturn(pdf);
        when(files.download(OWNER, pdf.id)).thenReturn("%PDF-garbage".getBytes());
        assertThatThrownBy(() -> new ImportExperienceCandidateService(files)
                .extract(OWNER, pdf.id)).isInstanceOf(BusinessException.class);
        verify(files, never()).upload(any(), any());
    }

    private static ImportedSource source(String type) {
        var source = new ImportedSource(OWNER, type, "example");
        source.id = UUID.randomUUID();
        return source;
    }

    private static byte[] pdf(String body) throws Exception {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            if (body != null) {
                try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                    content.beginText();
                    content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    content.newLineAtOffset(50, 700);
                    content.showText(body);
                    content.endText();
                }
            }
            try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                document.save(output);
                return output.toByteArray();
            }
        }
    }
}
