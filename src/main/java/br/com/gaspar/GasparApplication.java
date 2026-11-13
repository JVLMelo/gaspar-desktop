package br.com.gaspar;

import br.com.gaspar.domain.*;
import br.com.gaspar.report.ExcelReportService;
import br.com.gaspar.service.ConferenciaService;
import br.com.gaspar.service.NomeArquivoService;
import br.com.gaspar.update.UpdateService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public final class GasparApplication extends Application {
    private static final String MOON_ICON = "M21 12.79A9 9 0 1 1 11.21 3A7 7 0 0 0 21 12.79Z";
    private static final String SUN_ICON = "M12 4V2 M12 22V20 M4.93 4.93L3.51 3.51 "
            + "M20.49 20.49L19.07 19.07 M4 12H2 M22 12H20 "
            + "M4.93 19.07L3.51 20.49 M20.49 3.51L19.07 4.93 "
            + "M16 12A4 4 0 1 1 8 12A4 4 0 0 1 16 12Z";
    private static final Logger LOG = LoggerFactory.getLogger(GasparApplication.class);
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final NumberFormat MOEDA = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));

    private final ConferenciaService conferenciaService = new ConferenciaService();
    private final ExcelReportService excelService = new ExcelReportService();
    private final NomeArquivoService nomeArquivoService = new NomeArquivoService();
    private final UpdateService updateService = new UpdateService();
    private final FileSelectorPane dominioSelector = new FileSelectorPane("Relatório do Domínio", "Arraste o relatório do Domínio aqui");
    private final FileSelectorPane spedSelector = new FileSelectorPane("Relatório do SPED", "Arraste o relatório do SPED aqui");
    private final Button compareButton = new Button("Comparar relatórios");
    private final Button saveButton = new Button("⇩  Salvar Excel");
    private final ProgressBar progress = new ProgressBar(0);
    private final Label actionTitle = new Label("Aguardando os relatórios");
    private final Label status = new Label("Selecione os dois arquivos PDF para iniciar a conferência.");
    private final Label movement = new Label();
    private final HBox modeBox = new HBox(8);
    private final Label validationStep = new Label("Arquivos pendentes");
    private final Label layoutStep = new Label("Leiaute pendente");
    private final Label completionNotice = new Label();
    private final Label lastUpdated = new Label("Arquivos permanecem neste computador");
    private final TilePane summary = new TilePane(9, 9);
    private final TableView<NotaComparada> table = new TableView<>();
    private final ComboBox<String> filter = new ComboBox<>();
    private final TextField search = new TextField();
    private final Label count = new Label();
    private final VBox resultArea = new VBox(17);
    private final ObservableList<NotaComparada> allNotes = FXCollections.observableArrayList();
    private final FilteredList<NotaComparada> filtered = new FilteredList<>(allNotes);
    private BorderPane appRoot;
    private Image appIcon;
    private ResultadoComparacao currentResult;
    private long comparisonStartedAt;

    @Override
    public void start(Stage stage) {
        appIcon = new Image(getClass().getResourceAsStream("/icons/gaspar.png"));
        appRoot = new BorderPane();
        appRoot.getStyleClass().add("app-root");
        appRoot.setTop(buildTopBar());

        TilePane files = new TilePane(18, 18, dominioSelector, spedSelector);
        files.setPrefColumns(2);
        files.setPrefTileWidth(650);
        files.setTileAlignment(Pos.TOP_LEFT);
        files.setAlignment(Pos.TOP_LEFT);
        files.widthProperty().addListener((observable, previous, current) -> {
            double available = (current.doubleValue() - files.getHgap()) / 2;
            files.setPrefTileWidth(Math.max(400, available));
        });

        configureActions();
        configureTable();
        configureFilters();
        buildResultArea();

        VBox content = new VBox(20, buildPageToolbar(), files, buildActionCard(), resultArea);
        content.getStyleClass().add("content");
        content.setMaxWidth(1480);
        VBox.setVgrow(resultArea, Priority.ALWAYS);
        StackPane contentWrapper = new StackPane(content);
        contentWrapper.setAlignment(Pos.TOP_CENTER);
        contentWrapper.getStyleClass().add("content-wrapper");
        ScrollPane scroll = new ScrollPane(contentWrapper);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.getStyleClass().add("root-scroll");
        appRoot.setCenter(scroll);

        Scene scene = new Scene(appRoot, 1360, 860);
        scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
        stage.setTitle("Gaspar — Conferência Domínio × SPED");
        stage.getIcons().add(appIcon);
        stage.setMinWidth(960);
        stage.setMinHeight(680);
        stage.setScene(scene);
        stage.show();
        dominioSelector.onChange(this::updateCompareState);
        spedSelector.onChange(this::updateCompareState);
        checkForUpdates(stage);
    }

    private HBox buildTopBar() {
        ImageView mark = new ImageView(appIcon);
        // O PNG possui uma margem transparente generosa; o viewport mantém o
        // símbolo visualmente alinhado ao centro do texto no cabeçalho.
        mark.setViewport(new Rectangle2D(150, 150, 954, 954));
        mark.setFitWidth(44);
        mark.setFitHeight(44);
        mark.setPreserveRatio(true);
        mark.setSmooth(true);
        mark.getStyleClass().add("brand-icon");
        Label brandName = new Label("Gaspar");
        brandName.getStyleClass().add("brand-name");
        Label brandCaption = new Label("Conferência fiscal");
        brandCaption.getStyleClass().add("brand-caption");
        VBox brandCopy = new VBox(1, brandName, brandCaption);
        brandCopy.setAlignment(Pos.CENTER_LEFT);
        HBox brand = new HBox(12, mark, brandCopy);
        brand.setAlignment(Pos.CENTER_LEFT);
        brand.getStyleClass().add("brand");

        Label headerTitle = new Label("Conferência Domínio × SPED");
        headerTitle.getStyleClass().add("header-title");
        Label headerCaption = new Label("Compare relatórios fiscais localmente, com segurança e precisão");
        headerCaption.getStyleClass().add("header-caption");
        VBox headerCopy = new VBox(3, headerTitle, headerCaption);
        headerCopy.setAlignment(Pos.CENTER_LEFT);
        headerCopy.getStyleClass().add("header-copy");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        SVGPath themeIcon = new SVGPath();
        themeIcon.setContent(MOON_ICON);
        themeIcon.getStyleClass().add("theme-icon");
        Button theme = new Button("Tema escuro", themeIcon);
        theme.setGraphicTextGap(9);
        theme.getStyleClass().add("theme-button");
        theme.setOnAction(event -> {
            boolean dark = !appRoot.getStyleClass().contains("dark");
            appRoot.getStyleClass().remove("dark");
            if (dark) appRoot.getStyleClass().add("dark");
            themeIcon.setContent(dark ? SUN_ICON : MOON_ICON);
            theme.setText(dark ? "Tema claro" : "Tema escuro");
        });

        HBox topBar = new HBox(22, brand, headerCopy, spacer, theme);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setFillHeight(true);
        topBar.getStyleClass().add("topbar");
        return topBar;
    }

    private HBox buildPageToolbar() {
        Label title = new Label("Nova conferência");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Selecione os dois relatórios PDF para começar a validação.");
        subtitle.getStyleClass().add("page-subtitle");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label operationLabel = new Label("Tipo de operação");
        operationLabel.getStyleClass().add("mode-label");
        movement.getStyleClass().add("movement-badge");
        modeBox.getChildren().addAll(operationLabel, movement);
        modeBox.setAlignment(Pos.CENTER_RIGHT);
        modeBox.setVisible(false);
        modeBox.setManaged(false);
        VBox pageCopy = new VBox(4, title, subtitle);
        pageCopy.setAlignment(Pos.CENTER_LEFT);
        HBox toolbar = new HBox(pageCopy, spacer, modeBox);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("page-toolbar");
        return toolbar;
    }

    private void configureActions() {
        compareButton.getStyleClass().add("primary-button");
        compareButton.setDisable(true);
        compareButton.setOnAction(event -> compare());
        saveButton.getStyleClass().addAll("secondary-button", "export-button");
        saveButton.setDisable(true);
        progress.setMaxWidth(Double.MAX_VALUE);
        progress.getStyleClass().add("comparison-progress");
        actionTitle.getStyleClass().add("action-title");
        status.setWrapText(true);
        status.getStyleClass().add("status");
        validationStep.getStyleClass().add("step");
        layoutStep.getStyleClass().add("step");
    }

    private HBox buildActionCard() {
        VBox actionCopy = new VBox(2, actionTitle, status, progress);
        HBox.setHgrow(actionCopy, Priority.ALWAYS);
        HBox steps = new HBox(14, validationStep, layoutStep);
        steps.setAlignment(Pos.CENTER_RIGHT);
        HBox action = new HBox(17, compareButton, actionCopy, steps);
        action.setAlignment(Pos.CENTER_LEFT);
        action.getStyleClass().addAll("surface-card", "action-card");
        return action;
    }

    private void configureFilters() {
        filter.getItems().setAll("Todas", "Divergências", "OK", "Apenas no Domínio", "Apenas no SPED");
        filter.setValue("Divergências");
        filter.valueProperty().addListener((obs, before, after) -> applyFilter());
        filter.getStyleClass().add("filter-select");
        search.setPromptText("⌕  Buscar número da nota");
        search.textProperty().addListener((obs, before, after) -> applyFilter());
        search.getStyleClass().add("search-field");
        search.setMaxWidth(360);
        filter.setPrefWidth(190);
    }

    private void buildResultArea() {
        Label summaryTitle = sectionTitle("Resumo da conferência");
        Region summarySpacer = new Region();
        HBox.setHgrow(summarySpacer, Priority.ALWAYS);
        completionNotice.getStyleClass().add("completion-notice");
        HBox summaryHead = new HBox(summaryTitle, summarySpacer, completionNotice);
        summaryHead.setAlignment(Pos.CENTER_LEFT);
        summary.setPrefColumns(6);
        summary.setPrefTileWidth(185);
        summary.setTileAlignment(Pos.TOP_LEFT);

        Label tableTitle = sectionTitle("Notas conferidas");
        count.getStyleClass().addAll("count", "count-badge");
        Region tableSpacer = new Region();
        HBox.setHgrow(tableSpacer, Priority.ALWAYS);
        HBox tableHead = new HBox(12, tableTitle, count, tableSpacer, search, filter, saveButton);
        tableHead.setAlignment(Pos.CENTER_LEFT);
        tableHead.getStyleClass().add("table-head");

        HBox legend = new HBox(16, legend("OK", "legend-ok"), legend("Divergência", "legend-warning"), legend("Ausente", "legend-missing"));
        Region bottomSpacer = new Region();
        HBox.setHgrow(bottomSpacer, Priority.ALWAYS);
        lastUpdated.getStyleClass().add("last-updated");
        HBox tableBottom = new HBox(lastUpdated, bottomSpacer, legend);
        tableBottom.setAlignment(Pos.CENTER_LEFT);
        tableBottom.getStyleClass().add("table-bottom");
        VBox tableCard = new VBox(tableHead, table, tableBottom);
        tableCard.getStyleClass().addAll("surface-card", "table-card");
        VBox.setVgrow(table, Priority.ALWAYS);

        resultArea.getChildren().addAll(summaryHead, summary, tableCard);
        resultArea.setVisible(false);
        resultArea.setManaged(false);
        VBox.setVgrow(tableCard, Priority.ALWAYS);
    }

    private Label sectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-title");
        return label;
    }

    private Label legend(String text, String style) {
        Label label = new Label("●  " + text);
        label.getStyleClass().addAll("legend", style);
        return label;
    }

    private void compare() {
        Path dominio = dominioSelector.path();
        Path sped = spedSelector.path();
        if (dominio == null || sped == null) return;
        comparisonStartedAt = System.nanoTime();
        setBusy(true);
        saveButton.setDisable(true);
        modeBox.setVisible(false);
        modeBox.setManaged(false);
        status.getStyleClass().removeAll("error", "success");
        actionTitle.setText("Conferência em andamento");
        validationStep.setText("Validando arquivos");
        layoutStep.setText("Reconhecendo leiaute");
        validationStep.getStyleClass().remove("completed");
        layoutStep.getStyleClass().remove("completed");

        Task<ResultadoComparacao> task = new Task<>() {
            @Override protected ResultadoComparacao call() throws Exception {
                return conferenciaService.conferir(dominio, sped, this::updateMessage);
            }
        };
        status.textProperty().bind(task.messageProperty());
        task.setOnSucceeded(event -> {
            status.textProperty().unbind();
            currentResult = task.getValue();
            showResult(currentResult);
            setBusy(false);
            progress.setProgress(1);
            saveButton.setDisable(false);
            actionTitle.setText("Comparação concluída");
            validationStep.setText("✓  Arquivos validados");
            layoutStep.setText("✓  Leiaute reconhecido");
            validationStep.getStyleClass().add("completed");
            layoutStep.getStyleClass().add("completed");
            status.setText(currentResult.arquivosTrocados()
                    ? "Os arquivos estavam invertidos e foram ajustados automaticamente."
                    : "Os relatórios foram processados com sucesso.");
            status.getStyleClass().add("success");
        });
        task.setOnFailed(event -> {
            status.textProperty().unbind();
            setBusy(false);
            progress.setProgress(0);
            actionTitle.setText("Não foi possível concluir a conferência");
            Throwable error = task.getException();
            LOG.error("Falha ao executar a conferência", error);
            status.setText(userMessage(error));
            status.getStyleClass().add("error");
        });
        Thread.ofVirtual().name("gaspar-conferencia").start(task);
    }

    private void showResult(ResultadoComparacao result) {
        allNotes.setAll(result.notas());
        movement.setText(result.tipoMovimento().descricao());
        modeBox.setVisible(true);
        modeBox.setManaged(true);
        long elapsedMillis = (System.nanoTime() - comparisonStartedAt) / 1_000_000;
        completionNotice.setText("✓  Comparação concluída em " + formatDuration(elapsedMillis));
        lastUpdated.setText("Última atualização: hoje, " + LocalDateTime.now().format(HORA) + "  ·  Arquivos permanecem neste computador");
        buildSummary(result.resumo());
        applyFilter();
        resultArea.setVisible(true);
        resultArea.setManaged(true);
    }

    private String formatDuration(long milliseconds) {
        return "%02d:%02d".formatted(milliseconds / 1000, (milliseconds % 1000) / 10);
    }

    private void buildSummary(ResumoComparacao resumo) {
        summary.getChildren().clear();
        summary.getChildren().addAll(
                card("Notas no Domínio", Long.toString(resumo.notasDominio()), "documentos encontrados", "blue"),
                card("Notas no SPED", Long.toString(resumo.notasSped()), "documentos encontrados", "blue"),
                card("Iguais (OK)", Long.toString(resumo.iguais()), percent(resumo.iguais(), Math.max(resumo.notasDominio(), resumo.notasSped())), "good"),
                card("Apenas no Domínio", Long.toString(resumo.apenasDominio()), "notas não localizadas", resumo.apenasDominio() == 0 ? "good" : "bad"),
                card("Apenas no SPED", Long.toString(resumo.apenasSped()), "notas não localizadas", resumo.apenasSped() == 0 ? "good" : "bad"),
                card("Divergências", Long.toString(resumo.divergencias()), "valores diferentes", resumo.divergencias() == 0 ? "good" : "warn"),
                card("Valor total Domínio", MOEDA.format(resumo.valorTotalDominio()), "", "neutral"),
                card("Valor total SPED", MOEDA.format(resumo.valorTotalSped()), "", "neutral"),
                card("Diferença", MOEDA.format(resumo.diferenca()), "valor líquido", resumo.diferenca().signum() == 0 ? "good" : "warn")
        );
    }

    private String percent(long part, long total) {
        if (total == 0) return "0% da conferência";
        return String.format(Locale.of("pt", "BR"), "%.1f%% da conferência", part * 100.0 / total);
    }

    private VBox card(String label, String value, String caption, String tone) {
        Label textLabel = new Label(label);
        textLabel.getStyleClass().add("stat-label");
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("stat-value");
        Label captionLabel = new Label(caption);
        captionLabel.getStyleClass().add("stat-caption");
        VBox card = new VBox(2, textLabel, valueLabel, captionLabel);
        card.getStyleClass().addAll("surface-card", "stat-card", "stat-" + tone);
        return card;
    }

    private void applyFilter() {
        String selection = filter.getValue();
        String term = search.getText() == null ? "" : search.getText().strip();
        filtered.setPredicate(note -> {
            boolean category = switch (selection == null ? "Todas" : selection) {
                case "OK" -> note.situacao() == SituacaoComparacao.OK;
                case "Apenas no Domínio" -> note.situacao() == SituacaoComparacao.APENAS_DOMINIO;
                case "Apenas no SPED" -> note.situacao() == SituacaoComparacao.APENAS_SPED;
                case "Divergências" -> note.situacao() != SituacaoComparacao.OK;
                default -> true;
            };
            return category && (term.isBlank() || Long.toString(note.chave().nota()).contains(term));
        });
        count.setText(filtered.size() + " de " + allNotes.size() + " notas");
    }

    private void configureTable() {
        table.setItems(filtered);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("Nenhuma nota encontrada para este filtro."));
        table.getStyleClass().add("notes-table");
        addColumn("Nota", n -> "%09d".formatted(n.chave().nota()), 120, "note-column");
        addColumn("Série", n -> n.chave().serie(), 70, "center-column");
        addColumn("Data Domínio", NotaComparada::dataDominio, 125, this::formatDate, "center-column");
        addColumn("Espécie", NotaComparada::especieDominio, 85, this::dash, "center-column");
        addColumn("Valor Domínio", NotaComparada::valorDominio, 140, this::formatMoney, "numeric-column");
        addColumn("Data SPED", NotaComparada::dataSped, 125, this::formatDate, "center-column");
        addColumn("Modelo", NotaComparada::modeloSped, 85, this::dash, "center-column");
        addColumn("Valor SPED", NotaComparada::valorSped, 140, this::formatMoney, "numeric-column");
        addSituationColumn();
    }

    private void addSituationColumn() {
        TableColumn<NotaComparada, SituacaoComparacao> column = new TableColumn<>("Situação");
        column.getStyleClass().add("situation-column");
        column.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().situacao()));
        column.setMinWidth(180);
        column.setPrefWidth(195);
        column.setCellFactory(ignored -> new TableCell<>() {
            @Override protected void updateItem(SituacaoComparacao situation, boolean empty) {
                super.updateItem(situation, empty);
                setText(null);
                setGraphic(null);
                if (empty || situation == null) return;
                Label badge = new Label("●  " + situation.descricao());
                badge.getStyleClass().addAll("situation", switch (situation) {
                    case OK -> "situation-ok";
                    case DIVERGENCIA_VALOR -> "situation-warning";
                    case APENAS_DOMINIO, APENAS_SPED -> "situation-missing";
                });
                setGraphic(badge);
            }
        });
        table.getColumns().add(column);
    }

    private <T> void addColumn(String title, java.util.function.Function<NotaComparada, T> getter, double width, String style) {
        TableColumn<NotaComparada, T> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(getter.apply(data.getValue())));
        column.setPrefWidth(width);
        if (style != null) column.getStyleClass().add(style);
        table.getColumns().add(column);
    }

    private <T> void addColumn(String title, java.util.function.Function<NotaComparada, T> getter,
                               double width, java.util.function.Function<T, String> formatter, String style) {
        TableColumn<NotaComparada, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new ReadOnlyStringWrapper(formatter.apply(getter.apply(data.getValue()))));
        column.setPrefWidth(width);
        if (style != null) column.getStyleClass().add(style);
        table.getColumns().add(column);
    }

    private String formatDate(LocalDate date) { return date == null ? "—" : DATA.format(date); }
    private String formatMoney(BigDecimal value) { return value == null ? "—" : MOEDA.format(value); }
    private String dash(String value) { return value == null || value.isBlank() ? "—" : value; }

    private void saveExcel(Stage stage) {
        if (currentResult == null) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Salvar relatório da conferência");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Planilha Excel", "*.xlsx"));
        chooser.setInitialFileName(nomeArquivoService.sugerir(currentResult));
        File selected = chooser.showSaveDialog(stage);
        if (selected == null) return;
        Path destination = selected.toPath().toAbsolutePath();
        if (!destination.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            destination = destination.resolveSibling(destination.getFileName() + ".xlsx");
        }
        if (Files.exists(destination) && !confirmOverwrite(destination)) return;
        Path finalDestination = destination;
        setBusy(true);
        actionTitle.setText("Gerando o arquivo Excel");
        status.setText("Preparando as cinco abas da conferência…");
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                excelService.gerar(currentResult, finalDestination);
                return null;
            }
        };
        task.setOnSucceeded(event -> {
            setBusy(false);
            progress.setProgress(1);
            saveButton.setDisable(false);
            actionTitle.setText("Excel salvo com sucesso");
            status.setText(finalDestination.toString());
            status.getStyleClass().removeAll("error");
            status.getStyleClass().add("success");
        });
        task.setOnFailed(event -> {
            setBusy(false);
            progress.setProgress(0);
            saveButton.setDisable(false);
            LOG.error("Falha ao gerar Excel", task.getException());
            actionTitle.setText("Não foi possível salvar o Excel");
            status.setText("Verifique se o arquivo está aberto ou se a pasta permite escrita.");
            status.getStyleClass().add("error");
        });
        Thread.ofVirtual().name("gaspar-excel").start(task);
    }

    private boolean confirmOverwrite(Path destination) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "O arquivo " + destination.getFileName() + " já existe. Deseja substituí-lo?",
                ButtonType.CANCEL, ButtonType.OK);
        alert.setHeaderText("Confirmar substituição");
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private void checkForUpdates(Stage stage) {
        if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")
                || UpdateService.installedVersion() == null) return;

        Thread.ofVirtual().name("gaspar-update-check").start(() -> {
            try {
                updateService.checkForUpdate().ifPresent(update ->
                        Platform.runLater(() -> promptForUpdate(stage, update)));
            } catch (Exception e) {
                LOG.debug("Não foi possível verificar atualizações do Gaspar.", e);
            }
        });
    }

    private void promptForUpdate(Stage stage, UpdateService.Update update) {
        ButtonType updateButton = new ButtonType("Baixar e instalar", ButtonBar.ButtonData.OK_DONE);
        ButtonType laterButton = new ButtonType("Agora não", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(stage);
        alert.setTitle("Atualização do Gaspar");
        alert.setHeaderText("A versão " + update.latestVersion() + " está disponível");
        alert.setContentText("Versão instalada: " + update.currentVersion()
                + "\nA atualização será baixada do GitHub. O Gaspar será fechado para abrir o instalador.");
        alert.getButtonTypes().setAll(updateButton, laterButton);
        if (alert.showAndWait().orElse(laterButton).equals(updateButton)) {
            downloadAndInstallUpdate(stage, update);
        }
    }

    private void downloadAndInstallUpdate(Stage stage, UpdateService.Update update) {
        Alert progressAlert = new Alert(Alert.AlertType.INFORMATION);
        progressAlert.initOwner(stage);
        progressAlert.setTitle("Atualizando o Gaspar");
        progressAlert.setHeaderText("Baixando a versão " + update.latestVersion());
        progressAlert.setContentText("O instalador será verificado antes de ser aberto.");
        progressAlert.setGraphic(new ProgressIndicator());
        progressAlert.getButtonTypes().clear();
        progressAlert.show();

        Task<Path> task = new Task<>() {
            @Override protected Path call() throws Exception {
                return updateService.downloadAndVerify(update);
            }
        };
        task.setOnSucceeded(event -> {
            progressAlert.close();
            try {
                Path installer = task.getValue();
                Process installerProcess = new ProcessBuilder(installer.toString()).start();
                installerProcess.onExit().thenRun(() -> {
                    try {
                        Files.deleteIfExists(installer);
                        Files.deleteIfExists(installer.getParent());
                    } catch (IOException e) {
                        LOG.debug("Não foi possível remover o instalador temporário.", e);
                    }
                });
                Platform.exit();
            } catch (IOException e) {
                LOG.error("Não foi possível iniciar o instalador do Gaspar.", e);
                showUpdateError(stage, "O instalador foi baixado, mas não pôde ser aberto.");
            }
        });
        task.setOnFailed(event -> {
            progressAlert.close();
            LOG.error("Falha ao baixar a atualização do Gaspar.", task.getException());
            showUpdateError(stage, "Não foi possível baixar ou verificar o instalador. "
                    + "Confira sua conexão e tente novamente mais tarde.");
        });
        Thread.ofVirtual().name("gaspar-update-download").start(task);
    }

    private void showUpdateError(Stage stage, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.initOwner(stage);
        alert.setTitle("Falha na atualização");
        alert.setHeaderText("O Gaspar não foi atualizado");
        alert.showAndWait();
    }

    private void setBusy(boolean busy) {
        progress.setProgress(busy ? ProgressIndicator.INDETERMINATE_PROGRESS : progress.getProgress());
        compareButton.setDisable(busy || dominioSelector.path() == null || spedSelector.path() == null);
        dominioSelector.setDisable(busy);
        spedSelector.setDisable(busy);
        if (busy) saveButton.setDisable(true);
    }

    private void updateCompareState() {
        boolean ready = dominioSelector.path() != null && spedSelector.path() != null;
        compareButton.setDisable(!ready);
        validationStep.setText(ready ? "✓  Arquivos selecionados" : "Arquivos pendentes");
        if (ready) {
            validationStep.getStyleClass().add("completed");
            actionTitle.setText("Conferência pronta para iniciar");
            status.setText("Os dois relatórios serão identificados automaticamente.");
        } else {
            validationStep.getStyleClass().remove("completed");
            actionTitle.setText("Aguardando os relatórios");
            status.setText("Selecione os dois arquivos PDF para iniciar a conferência.");
        }
    }

    private String userMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null && (current.getMessage() == null || current.getMessage().isBlank())) {
            current = current.getCause();
        }
        return current.getMessage() == null ? "Ocorreu um erro inesperado durante a conferência." : current.getMessage();
    }

    public static void main(String[] args) { launch(args); }

    private static final class FileSelectorPane extends VBox {
        private final Label fileName = new Label();
        private final Label pathLabel = new Label();
        private final HBox fileRow = new HBox(10);
        private Path selected;
        private Runnable changeListener = () -> {};

        private FileSelectorPane(String title, String dropTitle) {
            setSpacing(14);
            setPadding(new Insets(20, 20, 16, 20));
            setPrefWidth(650);
            setMaxWidth(Double.MAX_VALUE);
            getStyleClass().addAll("surface-card", "upload-card");
            Label titleLabel = new Label(title);
            titleLabel.getStyleClass().add("upload-title");
            Region headSpacer = new Region();
            HBox.setHgrow(headSpacer, Priority.ALWAYS);
            Label required = new Label("Obrigatório · PDF");
            required.getStyleClass().add("required");
            HBox heading = new HBox(titleLabel, headSpacer, required);
            heading.setAlignment(Pos.CENTER_LEFT);

            Label dropHeading = new Label(dropTitle);
            dropHeading.getStyleClass().add("drop-title");
            Label dropCaption = new Label("ou selecione um arquivo do computador");
            dropCaption.getStyleClass().add("drop-caption");
            VBox dropCopy = new VBox(2, dropHeading, dropCaption);
            Button choose = new Button("Selecionar PDF");
            choose.getStyleClass().add("outline-button");
            choose.setOnAction(event -> chooseFile());
            Region dropSpacer = new Region();
            HBox.setHgrow(dropSpacer, Priority.ALWAYS);
            HBox drop = new HBox(13, pdfIcon(), dropCopy, dropSpacer, choose);
            drop.setAlignment(Pos.CENTER_LEFT);
            drop.setPadding(new Insets(13));
            drop.getStyleClass().add("drop-zone");

            fileName.getStyleClass().add("file-name");
            pathLabel.getStyleClass().add("file-path");
            pathLabel.setMaxWidth(410);
            pathLabel.setTextOverrun(OverrunStyle.CENTER_ELLIPSIS);
            VBox selectedCopy = new VBox(1, fileName, pathLabel);
            Region fileSpacer = new Region();
            HBox.setHgrow(fileSpacer, Priority.ALWAYS);
            Button remove = new Button("×");
            remove.setAccessibleText("Remover relatório");
            remove.getStyleClass().add("remove-button");
            remove.setOnAction(event -> select(null));
            fileRow.getChildren().addAll(pdfIcon(), selectedCopy, fileSpacer, remove);
            fileRow.setAlignment(Pos.CENTER_LEFT);
            fileRow.getStyleClass().add("file-row");
            fileRow.setVisible(false);
            fileRow.setManaged(false);
            getChildren().addAll(heading, drop, fileRow);
            configureDrop(drop);
        }

        private static Label pdfIcon() {
            Label pdf = new Label("PDF");
            pdf.getStyleClass().add("pdf-icon");
            return pdf;
        }

        private void chooseFile() {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Selecionar relatório em PDF");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Arquivos PDF", "*.pdf", "*.PDF"));
            File file = chooser.showOpenDialog(getScene() == null ? null : getScene().getWindow());
            if (file != null) select(file.toPath());
        }

        private void configureDrop(HBox drop) {
            drop.setOnDragOver(event -> {
                Dragboard board = event.getDragboard();
                if (board.hasFiles() && board.getFiles().size() == 1 && isPdf(board.getFiles().getFirst().toPath())) {
                    event.acceptTransferModes(TransferMode.COPY);
                    drop.getStyleClass().add("drag-active");
                }
                event.consume();
            });
            drop.setOnDragExited(event -> drop.getStyleClass().remove("drag-active"));
            drop.setOnDragDropped(event -> {
                drop.getStyleClass().remove("drag-active");
                List<File> files = event.getDragboard().getFiles();
                boolean accepted = files.size() == 1 && isPdf(files.getFirst().toPath());
                if (accepted) select(files.getFirst().toPath());
                event.setDropCompleted(accepted);
                event.consume();
            });
        }

        private boolean isPdf(Path path) { return path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pdf"); }

        private void select(Path path) {
            selected = path;
            fileName.setText(path == null ? "" : path.getFileName().toString());
            pathLabel.setText(path == null ? "" : path.toAbsolutePath().getParent().toString());
            fileRow.setVisible(path != null);
            fileRow.setManaged(path != null);
            changeListener.run();
        }

        private Path path() { return selected; }
        private void onChange(Runnable listener) { changeListener = listener; }
    }
}
