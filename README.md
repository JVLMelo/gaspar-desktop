# Gaspar Desktop

Aplicativo para comparar relatórios fiscais em PDF do Domínio e do SPED ICMS/IPI. O Gaspar funciona em Windows, Linux e macOS e exporta a conferência para Excel.

## O que o aplicativo compara

- relatórios de **Entradas** ou **Saídas**;
- notas agrupadas pelo número e pela série;
- valores com tolerância de R$ 0,01;
- notas encontradas em apenas um dos relatórios;
- divergências de valor.

Se os PDFs forem selecionados nos campos invertidos, o Gaspar tenta identificar e corrigir a ordem. Ele interrompe a conferência se encontrar linhas com aparência de nota que não conseguiu ler, para evitar um resultado parcial.

> Os PDFs precisam conter texto selecionável. O aplicativo não executa OCR em documentos que sejam somente imagens digitalizadas.

## Privacidade

Os PDFs são lidos e processados no computador. O conteúdo dos relatórios não é enviado ao GitHub nem incluído nos logs. No Windows, o aplicativo instalado consulta o GitHub ao iniciar para verificar se existe uma versão nova; o download do instalador só começa depois da confirmação do usuário. Se a verificação estiver sem internet, a conferência continua disponível.

Os logs ficam em `~/.gaspar/logs/gaspar.log` no Linux e macOS e em `%USERPROFILE%\.gaspar\logs\gaspar.log` no Windows.

## Baixar e instalar

Quando uma versão for publicada, os instaladores estarão na página [Releases](https://github.com/JVLMelo/gaspar-desktop/releases/latest).

| Sistema | Como obter |
| --- | --- |
| Windows | Instalador `.exe` na página Releases, com runtime Java incluído |
| Linux | Gere o pacote portátil `.tar.gz`, `.deb` ou `.rpm` em um computador Linux |
| macOS | Gere a imagem `.dmg` em um computador macOS |

O instalador Windows atualiza versões futuras do Gaspar. Quem estiver usando uma versão anterior ao atualizador integrado precisa instalar a primeira versão atualizada manualmente uma vez.

## Usar

1. Selecione ou arraste o PDF do Domínio.
2. Selecione ou arraste o PDF do SPED.
3. Clique em **Comparar relatórios**.
4. Revise o resumo e as notas sem correspondência ou com divergência.
5. Clique em **Salvar Excel** para exportar o resultado.

A planilha contém as abas `Resumo`, `Apenas no Domínio`, `Apenas no SPED`, `Divergências` e `Todas as Notas`.

## Executar a partir do código

Para compilar ou executar o projeto, instale o **JDK 21**. O Maven Wrapper baixa o Maven e as dependências na primeira execução; não é necessário instalar Maven globalmente.

### Linux e macOS

```bash
git clone https://github.com/JVLMelo/gaspar-desktop.git
cd gaspar-desktop
chmod +x mvnw
./mvnw clean test
./mvnw javafx:run
```

### Windows

No PowerShell:

```powershell
git clone https://github.com/JVLMelo/gaspar-desktop.git
cd gaspar-desktop
.\mvnw.cmd clean test
.\mvnw.cmd javafx:run
```

## Gerar pacotes

Os instaladores precisam ser montados no sistema operacional de destino, porque incluem runtime e bibliotecas nativas específicas da plataforma.

### Linux

```bash
./packaging/build-package.sh       # pacote portátil .tar.gz
./packaging/build-package.sh deb   # pacote .deb
./packaging/build-package.sh rpm   # pacote .rpm
```

### macOS

```bash
./packaging/build-package.sh       # imagem .dmg
```

### Windows

No PowerShell de um computador Windows com JDK 21 e as ferramentas requeridas pelo `jpackage`:

```powershell
powershell -ExecutionPolicy Bypass -File packaging\windows\build-installer.ps1 -PackageType exe
```

O instalador Windows também pode ser compilado a partir do Linux usando o GitHub Actions, que executa a compilação em um runner Windows. Para instruções detalhadas, consulte [packaging/README.md](packaging/README.md).

## Publicar uma versão Windows

1. Atualize a versão em `<version>` no `pom.xml`.
2. Envie as alterações para `main`.
3. Crie e envie uma tag com o mesmo número de versão, precedido por `v`.

Exemplo para a versão `1.1.1`:

```bash
git tag v1.1.1
git push origin v1.1.1
```

O workflow do GitHub Actions executa os testes, monta o instalador Windows, calcula seu checksum SHA-256 e publica ambos em uma Release. As instalações atuais do Gaspar no Windows verificam essa Release ao iniciar e oferecem a atualização quando ela é mais recente.

## Problemas e contribuições

Ao relatar um problema, informe o sistema operacional, a versão do Gaspar e a mensagem exibida. Não publique relatórios fiscais reais, planilhas de clientes, CPF, CNPJ, senhas ou tokens em issues ou pull requests. Para reproduzir problemas de leitura, prefira um exemplo sintético ou anonimizado.

## Licença

Este repositório ainda não contém um arquivo `LICENSE`; a licença do projeto precisa ser definida pelo mantenedor.
