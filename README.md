# Gaspar Desktop

Aplicação desktop multiplataforma em Java para comparar os relatórios de Entradas/Saídas do sistema Domínio com os Registros Fiscais do SPED ICMS/IPI. Funciona em Windows, Linux e macOS.

Todo o processamento acontece localmente. Os PDFs não são enviados para a internet e não é necessário executar navegador, API, Python ou Node.js.

## Requisitos para desenvolvimento

- JDK 21;
- acesso à internet apenas na primeira compilação, para baixar Maven e as dependências.

O Maven Wrapper incluído dispensa a instalação global do Maven.

## Clonar e instalar as dependências

Depois de clonar o repositório, é necessário ter apenas o **JDK 21** instalado. O Maven e todas as bibliotecas do projeto são baixados automaticamente pelos scripts `mvnw` e `mvnw.cmd`.

Confirme primeiro a versão do Java:

```bash
java -version
```

O resultado deve indicar Java 21.

### Linux ou macOS

```bash
git clone URL_DO_REPOSITORIO
cd gaspar-desktop
chmod +x mvnw
./mvnw clean package
```

Para executar o programa:

```bash
./mvnw javafx:run
```

### Windows

No PowerShell:

```powershell
git clone URL_DO_REPOSITORIO
cd gaspar-desktop
.\mvnw.cmd clean package
.\mvnw.cmd javafx:run
```

O comando `clean package` baixa automaticamente o JavaFX, Apache PDFBox, Apache POI, as bibliotecas de log, o JUnit e o Maven 3.9.11. As dependências ficam armazenadas no cache local do Maven, em `~/.m2/repository`, e não precisam ser baixadas novamente a cada compilação.

Para apenas instalar e utilizar o Gaspar, sem desenvolver o projeto, use o instalador ou pacote correspondente ao sistema operacional. Esses pacotes já incluem o Java e não exigem dependências adicionais no computador do usuário.

## Executar

Linux/macOS:

```bash
chmod +x mvnw
./mvnw javafx:run
```

Windows:

```powershell
mvnw.cmd javafx:run
```

## Testar e compilar

```bash
./mvnw clean test
./mvnw package
```

O JAR é criado em `target/gaspar-desktop-1.0.0.jar`. Para executar a interface, prefira `javafx:run` ou uma imagem empacotada, pois o JavaFX não faz parte do JDK padrão.

## Uso

1. Selecione ou arraste o PDF do Domínio.
2. Selecione ou arraste o PDF do SPED.
3. Clique em **Comparar**.
4. Consulte o resumo, filtre as notas e clique em **Salvar Excel**.

Se os arquivos forem selecionados na ordem inversa, o programa os identifica e corrige automaticamente. Relatórios de Entrada e Saída não podem ser misturados.

## Relatório Excel

O arquivo contém as abas `Resumo`, `Apenas no Domínio`, `Apenas no SPED`, `Divergências` e `Todas as Notas`.

## Pacotes para Windows, Linux e macOS

Cada pacote deve ser gerado no próprio sistema de destino, porque JavaFX contém componentes nativos diferentes em cada plataforma.

Linux — aplicação portátil `.tar.gz`:

```bash
./packaging/build-package.sh
```

Linux — pacote `.deb` ou `.rpm`:

```bash
./packaging/build-package.sh deb
./packaging/build-package.sh rpm
```

macOS — imagem `.dmg`:

```bash
./packaging/build-package.sh
```

Windows — instalador `.msi`:


```powershell
powershell -ExecutionPolicy Bypass -File packaging\windows\build-installer.ps1
```

### Gerar o instalador Windows usando Linux

O `jpackage` não gera um instalador Windows diretamente no Linux. Para contornar essa limitação de forma suportada, o projeto possui um workflow do GitHub Actions: o comando é iniciado no Linux, mas a compilação e o empacotamento acontecem em uma máquina Windows temporária do GitHub.

O resultado é um `.exe` autocontido, com o runtime Java incluído. Não é necessário possuir um computador Windows.

#### 1. Preparar o GitHub CLI

Instale o [GitHub CLI](https://cli.github.com/) no Linux. Em distribuições baseadas em Debian ou Ubuntu, ele normalmente pode ser instalado com:

```bash
sudo apt update
sudo apt install gh
```

Em seguida, autentique sua conta:

```bash
gh auth login
```

Escolha `GitHub.com`, o protocolo `HTTPS` e a autenticação pelo navegador quando essas opções forem apresentadas.

#### 2. Enviar o projeto ao GitHub

O serviço remoto só consegue compilar arquivos que já estejam no GitHub. Antes de gerar o instalador, confirme que as alterações foram salvas em um commit e enviadas:

```bash
git status
git push origin main
```

Na primeira utilização, o arquivo `.github/workflows/windows-exe.yml` também precisa estar presente na branch principal do repositório.

#### 3. Gerar e baixar o `.exe`

Execute na raiz do projeto:

```bash
chmod +x packaging/windows/build-on-github.sh
./packaging/windows/build-on-github.sh
```

O comando:

1. solicita uma máquina Windows no GitHub Actions;
2. configura o Java 21;
3. executa os testes automatizados;
4. gera o instalador com `jpackage` e WiX;
5. calcula o checksum SHA-256;
6. acompanha a execução até o final;
7. baixa o resultado automaticamente.

Ao terminar, os arquivos estarão em:

```text
target/windows-exe/
├── Gaspar-*.exe
└── SHA256SUMS.txt
```

É possível escolher outro diretório de saída passando-o como argumento:

```bash
./packaging/windows/build-on-github.sh "$HOME/Downloads/gaspar-windows"
```

O script interrompe a operação caso existam alterações locais sem commit, evitando gerar um instalador desatualizado.

#### Gerar pela interface do GitHub

O mesmo processo pode ser iniciado sem o GitHub CLI:

1. abra o repositório no GitHub;
2. entre na aba **Actions**;
3. escolha **Gerar EXE do Windows**;
4. clique em **Run workflow** e confirme a branch;
5. aguarde a conclusão da execução;
6. abra a execução concluída e baixe o artefato `Gaspar-Windows-*`.

O artefato permanece disponível no GitHub por 30 dias. Criar e enviar uma tag cujo nome comece com `v`, como `v1.0.0`, também inicia a geração automaticamente:

```bash
git tag v1.0.0
git push origin v1.0.0
```

Quando o empacotamento é executado diretamente no Windows, o resultado fica em `target/installer`. Quando é solicitado pelo script Linux, o download fica em `target/windows-exe`.

Detalhes e formatos alternativos estão em [packaging/README.md](packaging/README.md).

## Logs

Falhas técnicas são gravadas em `~/.gaspar/logs/gaspar.log`. Os PDFs e seus conteúdos completos não são registrados.

Consulte [planejamento.md](planejamento.md) para a arquitetura, critérios de aceite e evolução prevista.
