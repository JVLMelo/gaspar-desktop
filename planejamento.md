# Planejamento do Gaspar Desktop em Java

## 1. Objetivo

Criar uma aplicação desktop em Java para substituir a combinação atual de frontend React, API FastAPI e execução por linha de comando do projeto `gaspar`.

O programa será executado localmente, sem navegador, servidor, Railway, Vercel, Python ou Node.js. O usuário deverá selecionar dois relatórios em PDF — um do sistema Domínio e outro do SPED ICMS/IPI —, executar a conferência, visualizar o resultado na própria aplicação e salvar um relatório Excel.

O novo projeto deve manter as regras de negócio e os tratamentos de leiaute já existentes no projeto original, inclusive os casos especiais descobertos em uso real.

## 2. Escopo funcional identificado no projeto atual

### 2.1 Fluxo principal

1. Selecionar o PDF do relatório **Acompanhamento de Entradas/Saídas**, do Domínio.
2. Selecionar o PDF do relatório **Registros Fiscais**, do SPED ICMS/IPI.
3. Identificar automaticamente se os arquivos foram colocados nos campos invertidos e corrigir a ordem quando isso for inequívoco.
4. Detectar se cada relatório é de **entrada** ou **saída**.
5. Impedir a comparação quando um relatório for de entrada e o outro de saída.
6. Extrair as notas e os valores dos dois PDFs.
7. Agrupar linhas da mesma nota pela chave `(número da nota, série)`, somando os valores quando houver mais de um CFOP.
8. Comparar os dois conjuntos.
9. Exibir resumo e tabela detalhada na tela.
10. Gerar e salvar um arquivo Excel formatado.

### 2.2 Situações possíveis de uma nota

- `OK`: a nota existe nos dois relatórios e a diferença de valor é menor ou igual a R$ 0,01.
- `Apenas no Domínio`: a nota não foi encontrada no SPED.
- `Apenas no SPED`: a nota não foi encontrada no Domínio.
- `Divergência de valor`: os valores diferem em mais de R$ 0,01.

A data será exibida, mas não fará parte da chave nem determinará divergência, mantendo o comportamento atual.

### 2.3 Dados exibidos por nota

- número da nota;
- série;
- data no Domínio;
- espécie no Domínio;
- valor no Domínio;
- data de emissão no SPED;
- modelo no SPED;
- valor no SPED;
- situação da comparação.

Espécie e modelo são códigos originados de tabelas diferentes. Eles serão mostrados para apoio visual, mas não serão comparados nem usados como chave.

### 2.4 Resumo da conferência

- quantidade de notas no Domínio;
- quantidade de notas no SPED;
- quantidade de notas iguais;
- quantidade de notas apenas no Domínio;
- quantidade de notas apenas no SPED;
- quantidade de divergências de valor;
- valor total no Domínio;
- valor total no SPED;
- diferença entre os totais.

## 3. Decisões de arquitetura

### 3.1 Tecnologias propostas

- **Java 21 LTS** como versão mínima do JDK.
- **JavaFX** para a interface desktop.
- **Maven** para dependências, compilação, testes e empacotamento.
- **Apache PDFBox** para extração de texto e acesso às posições dos caracteres no PDF.
- **Apache POI** para criação do relatório `.xlsx`.
- **JUnit 5** para testes unitários e de integração.
- **TestFX**, opcionalmente, para os fluxos críticos de interface.
- **SLF4J + Logback** para logs locais e diagnóstico de PDFs com leiaute inesperado.
- **jlink/jpackage** para gerar uma distribuição com runtime Java próprio e instalador para Windows.

As versões exatas das bibliotecas deverão ser fixadas no `pom.xml` na implementação, após validar a compatibilidade entre Java, JavaFX, PDFBox e o plugin de empacotamento.

### 3.2 Tipo de aplicação

A solução será uma aplicação desktop monolítica e offline, dividida internamente em camadas. Não haverá API HTTP nem banco de dados no MVP.

```text
Interface JavaFX
       |
Serviço de conferência
       |
+------+-------------------+
|                          |
Leitores de PDF       Comparador
|                          |
Domínio / SPED         Resultado
       \                    /
        Gerador de Excel
```

Essa organização mantém interface, regras de negócio, leitura de arquivos e exportação desacopladas, facilitando testes e futuras alterações de leiaute.

### 3.3 Organização sugerida do projeto

```text
gaspar-desktop/
├── pom.xml
├── README.md
├── planejamento.md
├── src/
│   ├── main/
│   │   ├── java/br/com/gaspar/
│   │   │   ├── GasparApplication.java
│   │   │   ├── config/
│   │   │   ├── controller/
│   │   │   ├── domain/
│   │   │   ├── exception/
│   │   │   ├── parser/
│   │   │   ├── service/
│   │   │   ├── report/
│   │   │   └── util/
│   │   └── resources/
│   │       ├── fxml/
│   │       ├── css/
│   │       ├── images/
│   │       └── logback.xml
│   └── test/
│       ├── java/
│       └── resources/fixtures/
└── packaging/
    └── windows/
```

## 4. Modelo de domínio proposto

Usar tipos imutáveis, preferencialmente `record`, e `BigDecimal` para todos os valores monetários.

### 4.1 Tipos principais

- `TipoMovimento`: `ENTRADA` e `SAIDA`.
- `OrigemRelatorio`: `DOMINIO` e `SPED`.
- `SituacaoComparacao`: `OK`, `APENAS_DOMINIO`, `APENAS_SPED` e `DIVERGENCIA_VALOR`.
- `ChaveNota`: número da nota e série.
- `LinhaDominio`: nota, série, espécie, data e valor contábil.
- `LinhaSped`: nota, série, modelo, data de emissão e valor da operação.
- `NotaAgrupadaDominio`: dados consolidados por `ChaveNota`.
- `NotaAgrupadaSped`: dados consolidados por `ChaveNota`.
- `NotaComparada`: campos lado a lado e situação.
- `ResumoComparacao`: contagens, totais e diferença.
- `ResultadoComparacao`: tipo de movimento, notas comparadas, resumo e avisos.

### 4.2 Regras numéricas

- Valores de moeda devem usar `BigDecimal`, nunca `double` ou `float`.
- A conversão deve aceitar o formato brasileiro, como `1.234,56` e valores negativos.
- A soma deve preservar duas casas decimais.
- A divergência existe quando `abs(valorDominio - valorSped) > 0,01`.
- Quantidade de notas significa quantidade de chaves únicas `(nota, série)`, não quantidade de linhas de CFOP.

## 5. Leitura e identificação dos PDFs

### 5.1 Identificação dos arquivos

Criar um `IdentificadorRelatorioService` que leia a primeira página e procure títulos característicos:

- Domínio: `ACOMPANHAMENTO DE SAÍDAS` ou `ACOMPANHAMENTO DE ENTRADAS`;
- SPED: `REGISTROS FISCAIS`;
- SPED de entrada: `ENTRADAS DE MERCADORIAS`.

Se o campo Domínio contiver claramente um SPED e o campo SPED contiver claramente um Domínio, a aplicação deverá inverter os arquivos e informar isso discretamente ao usuário.

Se a origem não puder ser identificada com segurança, a comparação deve parar com uma mensagem clara, sem produzir resultado parcial.

### 5.2 Leitor do Domínio

O `DominioPdfParser` deverá:

- extrair texto preservando a separação visual entre colunas;
- reconhecer separadamente os leiautes de entrada e saída;
- extrair nota, série, espécie, data e valor contábil;
- ignorar linhas de totais e cabeçalhos;
- aceitar valores negativos;
- contar linhas que parecem notas, mas não correspondem ao padrão conhecido;
- abortar com erro de extração incompleta quando existir qualquer linha candidata não reconhecida.

#### Caso especial obrigatório: colunas sobrepostas

No relatório do Domínio, nomes longos de cliente ou fornecedor podem ser desenhados por cima de CFOP, UF e valor. Em casos severos, o texto extraído fica corrompido, embora os caracteres do valor continuem na posição horizontal correta.

O equivalente Java deverá reproduzir o fallback do projeto Python:

1. localizar no cabeçalho da página as coordenadas horizontais das colunas `Valor` e `Tipo`;
2. quando o padrão textual falhar em uma linha candidata, obter os caracteres e suas coordenadas pelo PDFBox;
3. manter apenas dígitos, ponto, vírgula e sinal situados dentro da faixa da coluna de valor;
4. aceitar o valor apenas se ele corresponder a um formato monetário válido;
5. se a recuperação não for confiável, interromper a conferência em vez de omitir a nota.

Como o PDFBox não possui a mesma API do `pdfplumber`, será necessário implementar uma subclasse de `PDFTextStripper` que capture `TextPosition` por linha. Essa é a parte tecnicamente mais sensível da migração e deverá ter testes com PDFs reais anonimizados.

### 5.3 Leitor do SPED

O `SpedPdfParser` deverá tratar cada nota como:

- uma linha de cabeçalho da nota; seguida de
- uma ou mais linhas de CFOP.

Para saídas, o cabeçalho contém data de emissão, data de saída opcional, número, modelo, série e situação. A segunda data deve ser opcional porque NFC-e modelo 65 pode apresentar somente a data de emissão.

Para entradas, o cabeçalho contém data de entrada, data de emissão, número, modelo, série, subsérie e dados do fornecedor.

Ao encontrar um cabeçalho que pareça uma nota, mas não corresponda ao leiaute estrito:

- limpar a nota corrente, evitando que CFOPs seguintes sejam atribuídos à nota anterior;
- contabilizar a falha;
- ao final, interromper a operação com erro de extração incompleta.

### 5.4 Contrato comum dos leitores

Criar uma interface semelhante a:

```java
public interface PdfReportParser<T> {
    ParsedReport<T> parse(Path arquivo) throws ReportParsingException;
}
```

`ParsedReport` deverá conter o tipo de movimento, as linhas extraídas e metadados úteis para diagnóstico. Exceções técnicas de PDF devem ser convertidas em mensagens compreensíveis para o usuário, preservando a causa no log.

## 6. Serviço de comparação

O `ComparacaoService` será independente de JavaFX, PDFBox e Apache POI.

Passos:

1. agrupar as linhas do Domínio por `ChaveNota`;
2. agrupar as linhas do SPED pela mesma chave;
3. somar os valores de todas as linhas/CFOPs da chave;
4. preservar a primeira data, espécie ou modelo, como no projeto atual;
5. criar a união de todas as chaves dos dois relatórios;
6. ordenar por número da nota e série;
7. classificar a situação;
8. calcular o resumo.

O serviço não deverá assumir que o número da nota é globalmente único; notas com mesmo número e séries diferentes são registros diferentes.

## 7. Interface desktop

### 7.1 Tela principal

A tela principal deverá conter:

- título `Conferência Domínio × SPED`;
- indicação do modo detectado: entradas ou saídas;
- dois seletores de PDF, um para Domínio e outro para SPED;
- suporte a botão `Selecionar` e arrastar/soltar arquivos;
- nome, caminho e opção de remover/substituir cada arquivo;
- botão `Comparar`, habilitado somente com dois arquivos válidos;
- barra ou indicador de progresso;
- área de mensagens e erros;
- cards com o resumo;
- botão `Salvar Excel`;
- tabela detalhada.

### 7.2 Tabela de resultados

Colunas:

1. Nota;
2. Série;
3. Data Domínio;
4. Espécie;
5. Valor Domínio;
6. Data SPED;
7. Modelo;
8. Valor SPED;
9. Situação.

Filtros mínimos:

- divergências, incluindo ausentes;
- todas;
- somente OK.

Melhorias recomendadas para o desktop:

- filtro específico por situação;
- busca por número da nota;
- ordenação ao clicar no cabeçalho;
- contador `X de Y notas`;
- copiar uma linha ou valor;
- cores equivalentes ao sistema atual: verde para OK, amarelo para divergência e vermelho para ausente.

### 7.3 Responsividade e processamento

A extração não pode bloquear a thread da interface. Usar `Task<ResultadoComparacao>` ou `Service<ResultadoComparacao>` do JavaFX e atualizar o progresso na FX Application Thread.

Enquanto a tarefa estiver em execução:

- desabilitar nova comparação;
- permitir cancelamento, se viável;
- mostrar a etapa atual: validação, leitura do Domínio, leitura do SPED, comparação ou preparação do resultado.

O resultado anterior só deve ser substituído após uma nova conferência bem-sucedida, ou ser explicitamente limpo ao iniciar conforme a decisão de UX documentada na implementação.

### 7.4 Mensagens de erro

As mensagens devem orientar a correção, por exemplo:

- arquivo não encontrado ou sem permissão de leitura;
- arquivo não é um PDF válido;
- relatório não reconhecido;
- relatórios de tipos diferentes;
- nenhuma nota extraída;
- leiaute inesperado e risco de resultado incompleto;
- falha ao salvar o Excel;
- arquivo Excel aberto/bloqueado por outro programa.

Detalhes técnicos e stack traces ficam somente no log.

## 8. Exportação para Excel

O `ExcelReportService` deverá gerar um `.xlsx` com cinco abas:

1. `Resumo`;
2. `Apenas no Domínio`;
3. `Apenas no SPED`;
4. `Divergências`;
5. `Todas as Notas`.

Formatação mínima:

- cabeçalho azul escuro, texto branco e negrito;
- primeira linha congelada;
- filtros automáticos;
- largura das colunas ajustada, com limite para textos muito longos;
- datas no padrão `dd/MM/yyyy`;
- moeda no padrão brasileiro com duas casas decimais;
- situação verde, amarela ou vermelha;
- planilhas sem dados ainda devem conter o cabeçalho.

Nome sugerido:

```text
CONFERENCIA_NOTAS_<MÊS>_<ANO>.xlsx
```

O mês e o ano deverão vir da menor data disponível entre os dois relatórios. Se nenhuma data válida existir, usar um nome neutro com data/hora da execução, evitando erro por valor nulo.

Ao clicar em `Salvar Excel`, abrir um `FileChooser` já preenchido com o nome sugerido. A aplicação não deve sobrescrever um arquivo existente sem confirmação.

## 9. Correspondência entre o projeto atual e o projeto Java

| Projeto atual | Responsabilidade | Componente Java proposto |
|---|---|---|
| `conferencia/dominio_parser.py` | leitura do PDF Domínio | `DominioPdfParser` |
| `conferencia/sped_parser.py` | leitura do PDF SPED | `SpedPdfParser` |
| `conferencia/comparador.py` | agrupamento, comparação e resumo | `ComparacaoService` |
| `conferencia/excel_report.py` | geração do Excel | `ExcelReportService` |
| `conferencia/utils.py` | identificação e nome de saída | `IdentificadorRelatorioService` e `NomeArquivoService` |
| `conferencia/erros.py` | falha segura de extração | hierarquia `ReportParsingException` |
| `frontend/src/App.tsx` | coordenação da tela | `MainController` |
| `Dropzone.tsx` | seleção dos PDFs | componente JavaFX de seleção/drag-and-drop |
| `ResumoCards.tsx` | resumo visual | painel de cards JavaFX |
| `NotasTable.tsx` | filtros e tabela | `TableView<NotaComparada>` |
| `api/server.py` | transporte HTTP e temporários | eliminado; chamada local direta |
| `main.py` | orquestração CLI | opcional; fora do MVP desktop |

## 10. Testes

### 10.1 Testes unitários

- conversão de valores brasileiros positivos e negativos;
- chave composta por nota e série;
- agrupamento de uma nota com vários CFOPs;
- classificação das quatro situações;
- tolerância exatamente em R$ 0,01;
- divergência acima de R$ 0,01;
- cálculo dos totais e da diferença;
- ordenação por nota e série;
- geração do nome do arquivo em todos os meses;
- detecção de entradas e saídas;
- reconhecimento de cabeçalho SPED com uma ou duas datas;
- rejeição de linhas candidatas não reconhecidas.

### 10.2 Testes de integração com PDFs

Manter fixtures pequenas e anonimizadas para:

- Domínio de saída comum;
- Domínio de entrada comum;
- Domínio com fornecedor de nome longo e valor sobreposto;
- SPED de saída modelo 55;
- SPED de saída modelo 65 sem data de saída;
- SPED de entrada;
- nota com múltiplos CFOPs;
- valores negativos;
- leiaute inesperado que deve falhar com segurança;
- arquivos Domínio/SPED informados em ordem inversa.

Os resultados extraídos devem ser comparados com valores conhecidos, e não apenas com a quantidade de linhas.

### 10.3 Testes do Excel

Abrir o arquivo gerado novamente com Apache POI e verificar:

- nomes e ordem das cinco abas;
- cabeçalhos;
- número de linhas;
- valores e tipos das células;
- formatos de data e moeda;
- filtros e congelamento da primeira linha;
- cores por situação.

### 10.4 Testes ponta a ponta

Executar o fluxo completo com pares reais anonimizados e comparar o resultado Java com o resultado produzido pela versão Python atual. A migração só deve ser considerada equivalente quando contagens, notas, situações e valores coincidirem.

## 11. Empacotamento e distribuição

O alvo principal deve ser Windows, pois os relatórios e o fluxo atual indicam esse ambiente.

### 11.1 Entregáveis

- instalador `.msi` ou `.exe`;
- runtime Java reduzido e embutido;
- atalho no menu Iniciar e, opcionalmente, na área de trabalho;
- ícone da aplicação;
- versão e fornecedor nos metadados;
- diretório de logs em uma pasta de dados do usuário;
- instruções curtas de instalação e uso.

### 11.2 Empacotamento

Usar `jpackage`, preferencialmente após criar uma imagem de runtime com `jlink`. O usuário final não deverá instalar Java, Maven ou qualquer outra dependência.

A geração do instalador Windows deve acontecer no Windows ou em pipeline compatível. Criar scripts Maven documentados para:

- executar testes;
- gerar o JAR/imagem da aplicação;
- gerar o instalador;
- executar uma verificação básica da distribuição.

## 12. Segurança, privacidade e robustez

- Todo o processamento será local e offline.
- Nenhum PDF ou resultado será enviado para serviços externos.
- PDFs serão abertos somente para leitura.
- A aplicação não deve manter cópias temporárias desnecessárias.
- Logs não devem registrar conteúdo integral de notas, nomes de clientes/fornecedores, CPF ou CNPJ.
- Validar tamanho, extensão e assinatura do PDF antes do processamento.
- Fechar documentos e streams com `try-with-resources`.
- Em caso de cancelamento ou erro, liberar arquivos para não impedir que o usuário os mova ou exclua.
- Nunca gerar silenciosamente um resultado parcial quando o parser detectar linhas suspeitas.

## 13. Fases de implementação

### Fase 0 — Preparação e linha de base

- catalogar PDFs reais de teste e anonimizar fixtures;
- executar a versão Python e salvar os resultados esperados;
- documentar versões de leiaute conhecidas do Domínio e do Visualizador SPED;
- criar o projeto Maven e o pipeline básico de testes.

**Critério de conclusão:** projeto compila e existe uma base reproduzível de resultados esperados.

### Fase 1 — Domínio e comparação

- criar modelos imutáveis e enums;
- implementar parsing monetário;
- implementar agrupamento, comparação e resumo;
- cobrir regras com testes unitários.

**Critério de conclusão:** todas as regras de comparação passam sem depender de PDF ou interface.

### Fase 2 — Parser do SPED

- implementar detecção de entrada/saída;
- implementar cabeçalhos de entrada e saída;
- tratar a segunda data opcional da NFC-e;
- associar e somar linhas de CFOP;
- implementar a rede de segurança para cabeçalhos inesperados.

**Critério de conclusão:** fixtures SPED produzem exatamente os dados esperados e falham com segurança em leiautes desconhecidos.

### Fase 3 — Parser do Domínio

- implementar detecção de entrada/saída;
- portar os padrões conhecidos;
- calibrar separação de colunas no PDFBox;
- implementar captura de `TextPosition` e recuperação posicional do valor;
- implementar a rede de segurança para linhas candidatas.

**Critério de conclusão:** fixtures comuns e de colunas sobrepostas produzem os mesmos valores da versão Python.

### Fase 4 — Orquestração e exportação

- implementar identificação e troca automática de arquivos;
- validar compatibilidade de tipos;
- implementar o serviço de conferência ponta a ponta;
- implementar o Excel e seus testes.

**Critério de conclusão:** o serviço recebe dois caminhos e gera resultado e Excel equivalentes aos atuais.

### Fase 5 — Interface JavaFX

- criar seleção e arrastar/soltar PDFs;
- criar execução assíncrona e progresso;
- criar resumo, filtros e tabela;
- criar fluxo para salvar o Excel;
- aplicar tema claro consistente e acessível;
- revisar mensagens de erro.

**Critério de conclusão:** o usuário realiza todo o fluxo sem terminal ou navegador.

### Fase 6 — Validação de equivalência

- rodar a suíte contra todos os pares de PDFs disponíveis;
- comparar o resultado Java ao Python campo a campo;
- testar arquivos grandes, cancelamento, permissões e Excel bloqueado;
- corrigir diferenças antes de remover a dependência operacional do sistema antigo.

**Critério de conclusão:** zero diferenças não justificadas nos casos conhecidos.

### Fase 7 — Instalador e entrega

- configurar `jlink` e `jpackage`;
- gerar instalador Windows;
- testar em máquina limpa e usuário sem privilégios administrativos, quando possível;
- escrever README de instalação, uso e solução de problemas;
- definir versão inicial, por exemplo `1.0.0`.

**Critério de conclusão:** aplicação instalada e executada em uma máquina sem Java, Python ou Node.js previamente instalados.

## 14. Critérios de aceite do MVP

O MVP estará pronto quando:

- funcionar totalmente sem internet e sem servidor local;
- aceitar PDFs de entradas e saídas;
- detectar arquivos invertidos;
- bloquear a comparação de tipos incompatíveis;
- reproduzir as extrações e comparações da versão Python nos PDFs conhecidos;
- tratar NFC-e sem segunda data;
- tratar valores sobrepostos no PDF do Domínio;
- nunca omitir silenciosamente uma linha suspeita;
- exibir resumo, filtros e tabela na interface;
- gerar as cinco abas do Excel com valores e formatação corretos;
- usar `BigDecimal` em toda a regra monetária;
- manter a interface responsiva durante o processamento;
- possuir testes automatizados para regras, parsers e Excel;
- ser distribuído como instalador com Java embutido.

## 15. Funcionalidades posteriores ao MVP

Estas ideias devem permanecer fora da primeira entrega para não aumentar o risco da migração:

- leitura direta do arquivo SPED `.txt`;
- processamento em lote de vários clientes ou períodos;
- histórico local de conferências, possivelmente com SQLite;
- comparação de datas como regra configurável;
- exportação também em PDF ou CSV;
- atualização automática do aplicativo;
- preferências de tema e diretório padrão;
- instaladores para Linux e macOS;
- suporte configurável a novos leiautes sem recompilar o programa.

## 16. Riscos e mitigação

| Risco | Impacto | Mitigação |
|---|---|---|
| PDFBox reconstruir linhas de forma diferente do `pdfplumber` | notas podem deixar de ser reconhecidas | testes com PDFs reais, ajuste por coordenadas e comparação campo a campo com Python |
| mudança de leiaute do Domínio ou SPED | extração incompleta | redes de segurança, erro explícito, logs diagnósticos e parsers isolados |
| uso de ponto flutuante para dinheiro | divergências falsas | `BigDecimal` desde a extração até o Excel |
| processamento bloquear a interface | experiência ruim ou aparência de travamento | JavaFX `Task`/`Service` e progresso por etapa |
| instalador depender de Java externo | falha na máquina do usuário | runtime próprio com `jlink`/`jpackage` |
| fixtures conterem dados fiscais sensíveis | risco de privacidade | anonimização e revisão antes de versionar |
| diferença entre ambiente de desenvolvimento e Windows final | erro apenas na entrega | pipeline e teste em máquina Windows limpa |
| resultado parcial parecer válido | decisão fiscal incorreta | interromper sempre que houver linha candidata não reconhecida |

## 17. Ordem recomendada de prioridade

1. Equivalência das regras de negócio.
2. Fidelidade dos parsers e falha segura.
3. Validação automatizada contra a versão atual.
4. Geração correta do Excel.
5. Interface simples e responsiva.
6. Instalador autocontido.
7. Melhorias visuais e funcionalidades adicionais.

Essa ordem reduz o principal risco do projeto: criar uma interface desktop agradável que produza uma conferência fiscal incompleta ou diferente da ferramenta já validada.
