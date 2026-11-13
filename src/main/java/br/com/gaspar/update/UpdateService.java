package br.com.gaspar.update;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UpdateService {
    private static final URI LATEST_RELEASE_API = URI.create(
            "https://api.github.com/repos/JVLMelo/gaspar-desktop/releases/latest");
    private static final String RELEASE_DOWNLOAD_BASE =
            "https://github.com/JVLMelo/gaspar-desktop/releases/download/";
    private static final Pattern TAG_NAME = Pattern.compile(
            "\"tag_name\"\\s*:\\s*\"v?(\\d+(?:\\.\\d+){1,3})\"");
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofMinutes(15);

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public Optional<Update> checkForUpdate() throws IOException, InterruptedException {
        String currentVersion = installedVersion();
        if (currentVersion == null) return Optional.empty();

        HttpRequest request = HttpRequest.newBuilder(LATEST_RELEASE_API)
                .timeout(CONNECT_TIMEOUT)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "Gaspar-Desktop-Updater")
                .GET()
                .build();
        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() == 404) return Optional.empty();
        if (response.statusCode() != 200) {
            throw new IOException("O GitHub respondeu com HTTP " + response.statusCode() + " ao verificar atualizações.");
        }

        Optional<String> latestVersion = extractLatestVersion(response.body());
        if (latestVersion.isEmpty() || compareVersions(latestVersion.get(), currentVersion) <= 0) {
            return Optional.empty();
        }

        String tag = "v" + latestVersion.get();
        String fileName = installerFileName(latestVersion.get());
        URI installer = URI.create(RELEASE_DOWNLOAD_BASE + tag + "/" + fileName);
        URI checksums = URI.create(RELEASE_DOWNLOAD_BASE + tag + "/SHA256SUMS.txt");
        return Optional.of(new Update(currentVersion, latestVersion.get(), installer, checksums, fileName));
    }

    public Path downloadAndVerify(Update update) throws IOException, InterruptedException {
        String checksums = getText(update.checksumsUri());
        String expectedHash = expectedHash(checksums, update.fileName());

        Path directory = Files.createTempDirectory("gaspar-update-");
        Path installer = directory.resolve(update.fileName());
        HttpRequest request = HttpRequest.newBuilder(update.installerUri())
                .timeout(REQUEST_TIMEOUT)
                .header("User-Agent", "Gaspar-Desktop-Updater")
                .GET()
                .build();
        HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(installer));
        if (response.statusCode() != 200) {
            Files.deleteIfExists(installer);
            throw new IOException("Não foi possível baixar o instalador (HTTP " + response.statusCode() + ").");
        }

        String actualHash = sha256(installer);
        if (!actualHash.equalsIgnoreCase(expectedHash)) {
            Files.deleteIfExists(installer);
            throw new IOException("A verificação de integridade do instalador falhou. O arquivo foi descartado.");
        }
        return installer;
    }

    static Optional<String> extractLatestVersion(String releaseJson) {
        Matcher matcher = TAG_NAME.matcher(releaseJson);
        if (!matcher.find()) return Optional.empty();
        return Optional.of(matcher.group(1));
    }

    static int compareVersions(String left, String right) {
        int[] leftParts = numericParts(left);
        int[] rightParts = numericParts(right);
        for (int i = 0; i < Math.max(leftParts.length, rightParts.length); i++) {
            int a = i < leftParts.length ? leftParts[i] : 0;
            int b = i < rightParts.length ? rightParts[i] : 0;
            int comparison = Integer.compare(a, b);
            if (comparison != 0) return comparison;
        }
        return 0;
    }

    public static String installedVersion() {
        String version = UpdateService.class.getPackage().getImplementationVersion();
        return version == null || version.isBlank() ? null : version;
    }

    private String getText(URI uri) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .header("User-Agent", "Gaspar-Desktop-Updater")
                .GET()
                .build();
        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new IOException("Não foi possível baixar a verificação de integridade (HTTP "
                    + response.statusCode() + ").");
        }
        return response.body();
    }

    private String expectedHash(String checksums, String fileName) throws IOException {
        Pattern linePattern = Pattern.compile("^([a-fA-F0-9]{64})\\s+\\*?"
                + Pattern.quote(fileName) + "$");
        for (String line : checksums.split("\\R")) {
            Matcher matcher = linePattern.matcher(line.replace("\uFEFF", "").strip());
            if (matcher.matches()) return matcher.group(1);
        }
        throw new IOException("O checksum do instalador não foi encontrado na publicação.");
    }

    private String sha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (var input = Files.newInputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 não está disponível neste Java.", e);
        }
    }

    private static String installerFileName(String version) {
        return "Gaspar-" + version + ".exe";
    }

    private static int[] numericParts(String version) {
        if (version == null || !version.matches("\\d+(?:\\.\\d+){1,3}")) {
            throw new IllegalArgumentException("Versão inválida: " + version);
        }
        String[] parts = version.split("\\.");
        int[] numbers = new int[parts.length];
        try {
            for (int i = 0; i < parts.length; i++) numbers[i] = Integer.parseInt(parts[i]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Versão inválida: " + version, e);
        }
        return numbers;
    }

    public record Update(String currentVersion, String latestVersion, URI installerUri,
                         URI checksumsUri, String fileName) {}
}
