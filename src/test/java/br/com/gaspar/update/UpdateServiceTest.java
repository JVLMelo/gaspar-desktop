package br.com.gaspar.update;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateServiceTest {
    @Test
    void extraiVersaoDaUltimaPublicacaoDoGithub() {
        String release = "{\"tag_name\":\"v1.2.3\",\"name\":\"Gaspar\"}";

        assertEquals("1.2.3", UpdateService.extractLatestVersion(release).orElseThrow());
    }

    @Test
    void ignoraRespostaSemTagDeVersao() {
        assertTrue(UpdateService.extractLatestVersion("{\"name\":\"sem tag\"}").isEmpty());
    }

    @Test
    void comparaVersoesNumericasSemConfundirPrefixos() {
        assertTrue(UpdateService.compareVersions("1.10.0", "1.9.9") > 0);
        assertTrue(UpdateService.compareVersions("1.1.1", "1.1.0") > 0);
        assertEquals(0, UpdateService.compareVersions("1.1.0", "1.1.0.0"));
        assertFalse(UpdateService.compareVersions("1.1.0", "1.1.1") > 0);
    }
}
