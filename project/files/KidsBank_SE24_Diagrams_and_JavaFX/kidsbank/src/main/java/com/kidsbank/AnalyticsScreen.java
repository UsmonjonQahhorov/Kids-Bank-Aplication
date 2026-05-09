package com.kidsbank;

import com.kidsbank.model.*;
import com.kidsbank.service.*;
import com.kidsbank.storage.JsonStorage;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Analytics dashboard with JavaFX charts for a child's account.
 * Group SE-24 - Virtual Bank for Kids
 */
public class AnalyticsScreen {

    private final String accountId;
    private final String userId;
    private final AccountService accountService;
    private final TaskService taskService;
    private final SavingsGoalService goalService;
    /** When true, Back returns to parent dashboard (parent viewing a child's analytics). */
    private final boolean returnToParentDashboard;
    private BorderPane root;

    private static final String CARD_STYLE = "-fx-background-color:white; -fx-background-radius:10; "
            + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.1),10,0,0,2);";

    public AnalyticsScreen(AccountService accountService, TaskService taskService,
                           SavingsGoalService goalService,
                           String accountId, String userId, boolean returnToParentDashboard) {
        this.accountService = accountService;
        this.taskService = taskService;
        this.goalService = goalService;
        this.accountId = accountId;
        this.userId = userId;
        this.returnToParentDashboard = returnToParentDashboard;
        buildUI();
    }

    private void buildUI() {
        root = new BorderPane();
        root.setStyle("-fx-background-color:#EBF3FB;");

        User child = JsonStorage.findUserById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found."));

        // ---- Header ----
        HBox header = new HBox();
        header.setPadding(new Insets(14, 20, 14, 20));
        header.setStyle("-fx-background-color:#1F4E79;");
        header.setAlignment(Pos.CENTER_LEFT);
        Label headerLbl = new Label("📊 Analytics — " + child.getName());
        headerLbl.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        headerLbl.setTextFill(Color.WHITE);
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Button backBtn = new Button("← Back");
        backBtn.setStyle("-fx-background-color:#2E75B6; -fx-text-fill:white; "
                + "-fx-font-weight:bold; -fx-background-radius:5;");
        backBtn.setOnAction(e -> {
            if (returnToParentDashboard) {
                MainApp.showParentDashboard();
            } else {
                MainApp.showChildDashboard();
            }
        });
        header.getChildren().addAll(headerLbl, sp, backBtn);
        root.setTop(header);

        List<Transaction> txs = accountService.getTransactions(accountId);

        // ---- Stat cards ----
        double totalEarned = txs.stream()
                .filter(t -> t.getType() == TransactionType.DEPOSIT || t.getType() == TransactionType.TASK_REWARD)
                .mapToDouble(Transaction::getAmount).sum();
        double totalSpent = txs.stream()
                .filter(t -> t.getType() == TransactionType.WITHDRAWAL
                        || t.getType() == TransactionType.SAVINGS_TRANSFER)
                .mapToDouble(Transaction::getAmount).sum();

        List<Task> childTasks = taskService.getTasksForChild(userId);
        long assigned = childTasks.size();
        long approved = childTasks.stream().filter(t -> t.getStatus() == TaskStatus.APPROVED).count();
        double taskRatePct = assigned == 0 ? 0 : (approved * 100.0 / assigned);

        List<SavingsGoal> activeGoals = goalService.getActiveGoals(accountId);
        double goalProgressPct = activeGoals.isEmpty() ? 0
                : activeGoals.get(0).getProgressPercent() * 100.0;

        HBox statsRow = new HBox(14);
        statsRow.setPadding(new Insets(16, 20, 8, 20));
        statsRow.setAlignment(Pos.CENTER);
        statsRow.getChildren().addAll(
                statCard("Total Earned", totalEarned, "#1E8449"),
                statCard("Total Spent", totalSpent, "#C0392B"),
                statCard("Task Completion", taskRatePct, "%", "#2E75B6"),
                statCard("Goal Progress", goalProgressPct, "%", "#1F4E79")
        );

        // ---- Charts grid ----
        GridPane grid = new GridPane();
        grid.setHgap(18);
        grid.setVgap(18);
        grid.setPadding(new Insets(8, 20, 20, 20));

        VBox barCard = chartCard("Weekly Summary", buildWeeklyBarChart(txs));
        VBox pieCard = chartCard("Spending Breakdown", buildSpendingPieChart(txs));
        VBox lineCard = chartCard("Balance Over Time", buildBalanceLineChart());

        grid.add(barCard, 0, 0);
        grid.add(pieCard, 1, 0);
        grid.add(lineCard, 0, 1);
        GridPane.setColumnSpan(lineCard, 2);

        ColumnConstraints cc1 = new ColumnConstraints();
        cc1.setPercentWidth(50);
        ColumnConstraints cc2 = new ColumnConstraints();
        cc2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(cc1, cc2);

        VBox main = new VBox(8, statsRow, grid);
        VBox.setVgrow(grid, Priority.ALWAYS);
        root.setCenter(main);
    }

    private VBox statCard(String title, double value, String color) {
        return statCard(title, value, "", color);
    }

    private VBox statCard(String title, double value, String suffix, String color) {
        VBox card = new VBox(4);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(14, 18, 14, 18));
        card.setMinWidth(150);
        card.setStyle(CARD_STYLE);
        String valStr = suffix.isEmpty()
                ? "$" + String.format("%.2f", value)
                : String.format("%.0f", value) + suffix;
        Label valLbl = new Label(valStr);
        valLbl.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        valLbl.setTextFill(Color.web(color));
        Label tLbl = new Label(title);
        tLbl.setFont(Font.font("Arial", 11));
        tLbl.setTextFill(Color.GRAY);
        card.getChildren().addAll(valLbl, tLbl);
        return card;
    }

    private VBox chartCard(String title, Region chartNode) {
        VBox box = new VBox(8);
        box.setPadding(new Insets(14));
        box.setStyle(CARD_STYLE);
        Label t = new Label(title);
        t.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        t.setTextFill(Color.web("#1F4E79"));
        chartNode.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(chartNode, Priority.ALWAYS);
        box.getChildren().addAll(t, chartNode);
        return box;
    }

    private BarChart<String, Number> buildWeeklyBarChart(List<Transaction> txs) {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setCategories(FXCollections.observableArrayList("Week 1", "Week 2", "Week 3", "Week 4"));
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Amount ($)");
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setLegendVisible(true);
        chart.setCategoryGap(10);
        chart.setAnimated(false);

        XYChart.Series<String, Number> earned = new XYChart.Series<>();
        earned.setName("Earned");
        XYChart.Series<String, Number> spent = new XYChart.Series<>();
        spent.setName("Spent");
        XYChart.Series<String, Number> saved = new XYChart.Series<>();
        saved.setName("Saved");

        LocalDate today = LocalDate.now();
        for (int w = 0; w < 4; w++) {
            LocalDate end = today.minusDays((3 - w) * 7);
            LocalDate start = end.minusDays(6);
            String label = "Week " + (w + 1);

            double earnedSum = 0;
            double spentSum = 0;
            double savedSum = 0;
            for (Transaction t : txs) {
                LocalDate d = t.getDateTime().toLocalDate();
                if (d.isBefore(start) || d.isAfter(end)) continue;
                switch (t.getType()) {
                    case DEPOSIT, TASK_REWARD -> earnedSum += t.getAmount();
                    case WITHDRAWAL -> spentSum += t.getAmount();
                    case SAVINGS_TRANSFER -> savedSum += t.getAmount();
                }
            }
            earned.getData().add(new XYChart.Data<>(label, earnedSum));
            spent.getData().add(new XYChart.Data<>(label, spentSum));
            saved.getData().add(new XYChart.Data<>(label, savedSum));
        }

        chart.getData().addAll(earned, spent, saved);
        chart.setPrefHeight(260);
        applyBarFill(earned, "#1E8449");
        applyBarFill(spent, "#C0392B");
        applyBarFill(saved, "#2E75B6");
        return chart;
    }

    private void applyBarFill(XYChart.Series<String, Number> series, String color) {
        for (XYChart.Data<String, Number> d : series.getData()) {
            d.nodeProperty().addListener((obs, o, n) -> {
                if (n != null) n.setStyle("-fx-bar-fill: " + color + ";");
            });
            if (d.getNode() != null) {
                d.getNode().setStyle("-fx-bar-fill: " + color + ";");
            }
        }
    }

    private PieChart buildSpendingPieChart(List<Transaction> txs) {
        double withdrawals = txs.stream()
                .filter(t -> t.getType() == TransactionType.WITHDRAWAL)
                .mapToDouble(Transaction::getAmount).sum();
        double rewards = txs.stream()
                .filter(t -> t.getType() == TransactionType.TASK_REWARD)
                .mapToDouble(Transaction::getAmount).sum();
        double savings = txs.stream()
                .filter(t -> t.getType() == TransactionType.SAVINGS_TRANSFER)
                .mapToDouble(Transaction::getAmount).sum();

        double total = withdrawals + rewards + savings;
        PieChart chart = new PieChart();
        chart.setLabelsVisible(true);
        chart.setLegendVisible(true);
        chart.setAnimated(false);
        chart.setPrefHeight(260);

        if (total <= 0) {
            chart.setData(FXCollections.observableArrayList(
                    new PieChart.Data("No spending data", 1)));
            return chart;
        }

        double pw = 100.0 * withdrawals / total;
        double pr = 100.0 * rewards / total;
        double ps = 100.0 * savings / total;

        chart.setData(FXCollections.observableArrayList(
                new PieChart.Data(String.format("Withdrawals (%.0f%%)", pw), withdrawals),
                new PieChart.Data(String.format("Task Rewards (%.0f%%)", pr), rewards),
                new PieChart.Data(String.format("Savings Transfers (%.0f%%)", ps), savings)
        ));
        return chart;
    }

    private LineChart<String, Number> buildBalanceLineChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Balance ($)");
        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setAnimated(false);
        chart.setCreateSymbols(true);
        chart.setLegendVisible(false);
        chart.setPrefHeight(260);

        List<Transaction> sorted = accountService.getTransactions(accountId).stream()
                .sorted(Comparator.comparing(Transaction::getDateTime))
                .collect(Collectors.toList());

        // One point per calendar day (end-of-day balance after last tx that day)
        Map<LocalDate, Transaction> lastByDay = new LinkedHashMap<>();
        for (Transaction t : sorted) {
            lastByDay.put(t.getDateTime().toLocalDate(), t);
        }
        List<LocalDate> days = new ArrayList<>(lastByDay.keySet());
        days.sort(LocalDate::compareTo);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Balance");
        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("dd MMM");
        for (LocalDate day : days) {
            Transaction t = lastByDay.get(day);
            series.getData().add(new XYChart.Data<>(day.format(dayFmt), t.getBalanceAfter()));
        }

        chart.getData().add(series);
        series.nodeProperty().addListener((obs, o, n) -> {
            if (n != null) n.setStyle("-fx-stroke:#2E75B6;");
        });
        return chart;
    }

    public BorderPane getRoot() {
        return root;
    }
}
