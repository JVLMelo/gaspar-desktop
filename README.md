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

Os scripts executam os testes, reúnem o JAR e suas dependências e chamam `jpackage`. O resultado fica em `target/installer`. O runtime Java é incluído, portanto o usuário final não precisa ter Java instalado.

Detalhes e formatos alternativos estão em [packaging/README.md](packaging/README.md).

## Logs

Falhas técnicas são gravadas em `~/.gaspar/logs/gaspar.log`. Os PDFs e seus conteúdos completos não são registrados.

Consulte [planejamento.md](planejamento.md) para a arquitetura, critérios de aceite e evolução prevista.
