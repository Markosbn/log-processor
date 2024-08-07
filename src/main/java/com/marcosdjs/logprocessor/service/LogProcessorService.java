package com.marcosdjs.logprocessor.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class LogProcessorService {

    private static final Logger log = LoggerFactory.getLogger(LogProcessorService.class);

    private static final Path PATH = Path.of("temp");

    public File processarArquivosLogs(List<MultipartFile> arquivosLogs, List<Long> idsNfesBuscados) {
        if (ObjectUtils.isEmpty(idsNfesBuscados)) {
            throw new RuntimeException("Ids não informados");
        }
        if (ObjectUtils.isEmpty(arquivosLogs)){
            throw new RuntimeException("Arquivos não informados");
        }

        List<String> idsString = idsNfesBuscados.stream()
                .map(Object::toString)
                .toList();
        log.info("Quantidade de arquivos: {}", arquivosLogs.size());
        buscarValorIdNosLogsGerarArquivoParaProcessamento(arquivosLogs, idsString);

        try {
            var nomeArquivoZipado = "arquivos_reprocessar.zip";
            if (Files.notExists(PATH)) return null;

            String zipGerado = PATH + File.separator + nomeArquivoZipado;
            Files.deleteIfExists(Path.of(zipGerado));

            List<String> listFileZipar = generateFileList(PATH.toString(), new File(PATH.toString()));

            this.ziparArquivos(listFileZipar, zipGerado);

            return Path.of(zipGerado).toFile();
        } catch (IOException e ){
            e.printStackTrace();
        }
        return null;
    }

    private void ziparArquivos(List<String> fileList, String outputZipFile) throws IOException {
        byte[] buffer = new byte[1024];
        FileOutputStream fos = new FileOutputStream(outputZipFile);
        ZipOutputStream zos = new ZipOutputStream(fos);
        fileList.forEach(file -> {
            var pathFile = PATH + File.separator + file;
            ZipEntry ze = new ZipEntry(file);
            try {
                zos.putNextEntry(ze);
                FileInputStream in = new FileInputStream(pathFile);
                int len;
                while ((len = in.read(buffer)) > 0) {
                    zos.write(buffer, 0, len);
                }
                in.close();
                Files.deleteIfExists(Path.of(pathFile));
            } catch (IOException e) {
                throw new RuntimeException("Não foi possível compactar os Arquivos!", e);
            }
        });

        zos.closeEntry();
        zos.close();
        log.info("Gerado arquivo ZIP das notas fiscais em {}", outputZipFile);
    }

    private List<String> generateFileList(String sourceFolder, File node) {
        List<String> fileList = new ArrayList<>();
        if (node.isDirectory()) {
            String[] subNote = node.list();
            for (String filename: subNote) {
                var file = new File(node, filename);
                if (file.isFile()) {
                    fileList.add(generateZipEntry(sourceFolder, file.toString()));
                }
            }
        }
        return fileList;
    }

    private String generateZipEntry(String sourceFolder, String file) {
        return file.substring(sourceFolder.length() + 1, file.length());
    }

    private void buscarValorIdNosLogsGerarArquivoParaProcessamento(List<MultipartFile> arquivos, List<String> ids) {
        var pathVarificacao = "Monitoramento\\Saida";
        var fileExtensionVerificacao = "resp.env";

        AtomicInteger quantityFilesErrors = new AtomicInteger(0);
        try {
            this.limparPasta();
        }catch (IOException e){
            log.info("Erro ao limpar pasta" + e.getMessage());
        }
        AtomicInteger contadorTeste = new AtomicInteger(0);
        arquivos.forEach(file -> {
            contadorTeste.getAndIncrement();
            log.info("Processando arquivo {} numero arquivo: {}", file.getOriginalFilename(), contadorTeste.get());
            File tempFile = null;
            try {
                tempFile = File.createTempFile("log", file.getOriginalFilename());
                file.transferTo(tempFile);

                try (BufferedReader br = new BufferedReader(new FileReader(tempFile))) {
                    String linha;
                    while ((linha = br.readLine()) != null) {
                        for (String idNfe : ids) {
                            List<String> linhasParaProcessar = new ArrayList<>();
                            String nomeArquivo = idNfe;
                            if (linha.contains(idNfe) &&
                                    linha.contains(pathVarificacao) &&
                                    linha.contains(fileExtensionVerificacao)) {
                                log.info("Linha encontrada:" + linha);
                                nomeArquivo = linha.substring(linha.indexOf(idNfe));

                                boolean controleLinhas = true;
                                while (controleLinhas) {
                                    var linhaNova = br.readLine();
                                    linhasParaProcessar.add(linhaNova);
                                    if (linhaNova.startsWith("XML")) controleLinhas = false;
                                }

                                salvarResultadosEmArquivo(linhasParaProcessar, nomeArquivo);
                                break;
                            }

                        }
                    }
                } catch (IOException | IllegalStateException e) {
                    quantityFilesErrors.getAndIncrement();
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            } catch (IOException | IllegalStateException e ) {
                e.printStackTrace();
                throw new RuntimeException(e);
            } finally {
                if (tempFile != null && tempFile.exists()) {
                    var isDeleted = tempFile.delete();
                    if (!isDeleted) {
                        log.info("Ocorreu um erro ao deletar arquivo temporario.");
                    }
                }
            }


        });
    }

    private void limparPasta() throws IOException {
        if (Files.exists(PATH)) {
            File[] files = PATH.toFile().listFiles();
            for (File file : files) {
                file.delete();
            }
            Files.deleteIfExists(PATH);
        }

    }

    private void salvarResultadosEmArquivo(List<String> linhas, String nomeArquivoSaida) throws IOException {
        Files.createDirectories(PATH);

        File arquivoProcessado = new File(String.valueOf(PATH.resolve(nomeArquivoSaida).toFile()));
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(arquivoProcessado))) {
            if (!linhas.isEmpty()){
                for (String linha : linhas) {
                    log.info("Processando linha:" + linha);
                    bw.write(linha);
                    bw.newLine();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
