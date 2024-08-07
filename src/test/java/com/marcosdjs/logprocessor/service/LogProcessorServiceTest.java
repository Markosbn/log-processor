package com.marcosdjs.logprocessor.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class LogProcessorServiceTest {

    @Autowired
    private LogProcessorService service;

    @Test
    void processarArquivosLogs() {
        List<Long> idsBuscados = List.of(5772354L, 5772350L);

        File file = assertDoesNotThrow(() -> service.processarArquivosLogs(this.gerarListMultiPartFiles(), idsBuscados));
        Assertions.assertNotNull(file);
        Assertions.assertTrue(file.exists());
        Assertions.assertEquals("arquivos_reprocessar.zip", file.getName());
        Assertions.assertTrue(file.length() > 0);
        this.assertionFileContent(file);
    }

    private void assertionFileContent(File file) {
        try (ZipFile zipFile = new ZipFile(file)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory()) {
                    Assertions.assertTrue(
                            (entry.getName().contains("5772354") || entry.getName().contains("5772350")) &&
                                    entry.getName().contains(".env")
                    );
                    try (InputStream is = zipFile.getInputStream(entry);
                         BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                        String linha;
                        int numeroLinha = 0;
                        while ((linha = br.readLine()) != null) {
                            assertFalse(linha.isEmpty());
                            Assertions.assertTrue(
                                    linha.startsWith("OK") || linha.startsWith("[ENVIO]") ||
                                            linha.startsWith("Arquivo") || linha.startsWith("CodigoDeRetorno") ||
                                            linha.startsWith("NumeroSessao") || linha.startsWith("Resultado") ||
                                            linha.startsWith("RetornoStr") || linha.startsWith("XML")
                            );
                            switch (numeroLinha) {
                                case 0 -> Assertions.assertTrue(linha.startsWith("OK"));
                                case 1 -> Assertions.assertTrue(linha.startsWith("[ENVIO]"));
                                case 2 -> Assertions.assertTrue(linha.startsWith("Arquivo"));
                                case 3 -> Assertions.assertTrue(linha.startsWith("CodigoDeRetorno"));
                                case 4 -> Assertions.assertTrue(linha.startsWith("NumeroSessao"));
                                case 5 -> Assertions.assertTrue(linha.startsWith("Resultado"));
                                case 6 -> Assertions.assertTrue(linha.startsWith("RetornoStr"));
                                case 7 -> Assertions.assertTrue(linha.startsWith("XML"));
                            }
                            numeroLinha++;
                        }
                    }
                } else{
                    Assertions.assertEquals("Sem diretorios esperados", "possui diretorios");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<MultipartFile> gerarListMultiPartFiles(){
        List<MultipartFile> filesLogs = new ArrayList<>();
        try {
            File file = new File("src/test/resources/data/");
            if (file.isDirectory()) {
                String[] files = file.list();
                for (String fileName : files) {
                    File arquivoProcessar = new File(file, fileName);
                    filesLogs.add(new MockMultipartFile(arquivoProcessar.getName(), arquivoProcessar.getName(),"application/*", new FileInputStream(arquivoProcessar)));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            Assertions.assertFalse(true);
        }
        return filesLogs;
    }
}