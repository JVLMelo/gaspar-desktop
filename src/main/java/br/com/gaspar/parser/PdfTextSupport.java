package br.com.gaspar.parser;

import br.com.gaspar.exception.ReportParsingException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public final class PdfTextSupport {
    private static final byte[] PDF_MAGIC = {'%', 'P', 'D', 'F', '-'};

    private PdfTextSupport() {}

    public static void validarPdf(Path arquivo) throws ReportParsingException {
        if (arquivo == null || !Files.isRegularFile(arquivo)) {
            throw new ReportParsingException("Arquivo PDF não encontrado: " + arquivo);
        }
        if (!Files.isReadable(arquivo)) {
            throw new ReportParsingException("Sem permissão para ler o arquivo: " + arquivo);
        }
        try (var input = Files.newInputStream(arquivo)) {
            byte[] inicio = input.readNBytes(PDF_MAGIC.length);
            if (!Arrays.equals(inicio, PDF_MAGIC)) {
                throw new ReportParsingException("O arquivo selecionado não é um PDF válido: " + arquivo.getFileName());
            }
        } catch (IOException e) {
            throw new ReportParsingException("Não foi possível ler o arquivo: " + arquivo.getFileName(), e);
        }
    }

    public static String primeiraPagina(Path arquivo) throws ReportParsingException {
        validarPdf(arquivo);
        try (PDDocument document = Loader.loadPDF(arquivo.toFile())) {
            if (document.getNumberOfPages() == 0) return "";
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(1);
            stripper.setEndPage(1);
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        } catch (IOException e) {
            throw new ReportParsingException("Não foi possível extrair o texto de " + arquivo.getFileName(), e);
        }
    }

    public static String textoCompleto(Path arquivo) throws ReportParsingException {
        validarPdf(arquivo);
        try (PDDocument document = Loader.loadPDF(arquivo.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        } catch (IOException e) {
            throw new ReportParsingException("Não foi possível extrair o texto de " + arquivo.getFileName(), e);
        }
    }
}
