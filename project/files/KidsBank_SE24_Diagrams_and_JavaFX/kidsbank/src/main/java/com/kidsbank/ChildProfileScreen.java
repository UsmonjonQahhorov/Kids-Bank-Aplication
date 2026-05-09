package com.kidsbank;

import com.kidsbank.model.*;
import com.kidsbank.service.*;
import com.kidsbank.storage.JsonStorage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Parent view listing all registered children with balances and quick actions.
 */
public class ChildProfileScreen {

    private final AuthService authService;
    private final AccountService accountService;
    private final TaskService taskService;
    private final SavingsGoalService goalService;
    private BorderPane root;

    public ChildProfileScreen(AuthService authService, AccountService accountService,
                            TaskService taskService, SavingsGoalService goalService) {
        this.authService = authService;
        this.accountService = accountService;
        this.taskService = taskService;
        this.goalService = goalService;
        buildUI();
    }

    private void buildUI() {
        root = new BorderPane();
        root.setStyle("-fx-background-color:#EBF3FB;");

        HBox header = new HBox();
        header.setPadding(new Insets(14, 20, 14, 20));
        header.setStyle("-fx-background-color:#1F4E79;");
        header.setAlignment(Pos.CENTER_LEFT);
        Label hLbl = new Label("👧 View Children");
        hLbl.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        hLbl.setTextFill(Color.WHITE);
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Button backBtn = new Button("← Back");
        backBtn.setStyle("-fx-background-color:#2E75B6; -fx-text-fill:white; "
                + "-fx-font-weight:bold; -fx-background-radius:5;");
        backBtn.setOnAction(e -> MainApp.showParentDashboard());
        header.getChildren().addAll(hLbl, sp, backBtn);
        root.setTop(header);

        VBox content = new VBox(14);
        content.setPadding(new Insets(18));
        List<User> children = JsonStorage.loadAllUsers().stream()
                .filter(u -> "child".equalsIgnoreCase(u.getRole()))
                .collect(Collectors.toList());

        if (children.isEmpty()) {
            Label empty = new Label("No children registered yet.");
            empty.setFont(Font.font("Arial", 13));
            empty.setTextFill(Color.GRAY);
            content.getChildren().add(empty);
        } else {
            for (User child : children) {
                content.getChildren().add(childCard(child));
            }
        }

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:#EBF3FB;");
        root.setCenter(scroll);
    }

    private VBox childCard(User child) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(14, 18, 14, 18));
        card.setStyle("-fx-background-color:white; -fx-background-radius:10; "
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.1),10,0,0,2);");

        Label nameLbl = new Label(child.getName() + " — age " + child.getAge());
        nameLbl.setFont(Font.font("Arial", FontWeight.BOLD, 15));
        nameLbl.setTextFill(Color.web("#1F4E79"));

        List<Account> accs = accountService.getAccountsForUser(child.getUserId());
        double bal = accs.isEmpty() ? 0 : accs.stream().mapToDouble(Account::getBalance).sum();
        Label balLbl = new Label("Balance: $" + String.format("%.2f", bal));
        balLbl.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        balLbl.setTextFill(Color.web("#1E8449"));

        long pending = taskService.getTasksForChild(child.getUserId()).stream()
                .filter(t -> t.getStatus() == TaskStatus.PENDING).count();

        int activeGoals = 0;
        for (Account a : accs) {
            activeGoals += goalService.getActiveGoals(a.getAccountId()).size();
        }

        Label statsLbl = new Label("Pending tasks: " + pending + "  |  Active savings goals: " + activeGoals);
        statsLbl.setFont(Font.font("Arial", 11));
        statsLbl.setTextFill(Color.GRAY);

        HBox row = new HBox(10);
        row.setPadding(new Insets(4, 0, 0, 0));
        Button histBtn = MainApp.makePrimaryBtn("📜 View History");
        Button pinBtn = new Button("🔑 Reset PIN");
        pinBtn.setStyle("-fx-background-color:#616A6B; -fx-text-fill:white; "
                + "-fx-font-weight:bold; -fx-background-radius:6; -fx-padding:8 14;");

        Account primary = accs.isEmpty() ? null : accs.get(0);
        histBtn.setDisable(primary == null);
        histBtn.setOnAction(e -> {
            if (primary == null) return;
            MainApp.showTransactionHistory(primary.getAccountId(), child.getName());
        });

        pinBtn.setOnAction(e -> showResetPinDialog(child));

        row.getChildren().addAll(histBtn, pinBtn);

        card.getChildren().addAll(nameLbl, balLbl, statsLbl, row);
        return card;
    }

    private void showResetPinDialog(User child) {
        Dialog<ButtonType> d = new Dialog<>();
        MainApp.styleDialogPane(d.getDialogPane());
        d.setTitle("Reset PIN");
        d.setHeaderText("Set a new 4-digit PIN for " + child.getName());

        PasswordField p1 = new PasswordField();
        p1.setPromptText("New 4-digit PIN");
        PasswordField p2 = new PasswordField();
        p2.setPromptText("Confirm PIN");

        VBox box = new VBox(10, new Label("New PIN:"), p1, new Label("Confirm:"), p2);
        box.setPadding(new Insets(10));
        d.getDialogPane().setContent(box);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        d.showAndWait().ifPresent(bt -> {
            if (bt != ButtonType.OK) return;
            try {
                String a = p1.getText().trim();
                String b = p2.getText().trim();
                if (!a.equals(b)) {
                    MainApp.showError("PINs do not match.");
                    return;
                }
                if (!authService.resetPin(child.getUserId(), a)) {
                    MainApp.showError("Could not reset PIN.");
                    return;
                }
                MainApp.showInfo("PIN Updated", child.getName() + "'s PIN has been reset.");
                MainApp.showChildProfiles();
            } catch (Exception ex) {
                MainApp.showError(ex.getMessage());
            }
        });
    }

    public BorderPane getRoot() {
        return root;
    }
}
