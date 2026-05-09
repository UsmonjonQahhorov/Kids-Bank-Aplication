package com.kidsbank;

import com.kidsbank.model.*;
import com.kidsbank.service.*;
import com.kidsbank.storage.JsonStorage;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Main dashboard shown to logged-in parents.
 * Group SE-24 - Virtual Bank for Kids
 */
public class ParentDashboardScreen {

    private final AuthService authService;
    private final AccountService accountService;
    private final TaskService taskService;
    private BorderPane root;

    public ParentDashboardScreen(AuthService authService, AccountService accountService,
                                  TaskService taskService) {
        this.authService    = authService;
        this.accountService = accountService;
        this.taskService    = taskService;
        buildUI();
    }

    private void buildUI() {
        root = new BorderPane();
        root.getStyleClass().add("kidsbank-page");
        root.setStyle("-fx-background-color:#EBF3FB;");

        User parent = authService.getCurrentUser();

        // ---- HEADER ----
        HBox header = new HBox();
        header.setPadding(new Insets(14, 20, 14, 20));
        header.setStyle("-fx-background-color:#1F4E79;");
        header.setAlignment(Pos.CENTER_LEFT);
        Label hLbl = new Label("🏦  Parent Dashboard — Welcome, " + parent.getName());
        hLbl.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        hLbl.setTextFill(Color.WHITE);
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Button logoutBtn = new Button("Logout");
        logoutBtn.setStyle("-fx-background-color:#C0392B; -fx-text-fill:white; "
                + "-fx-font-weight:bold; -fx-background-radius:5;");
        logoutBtn.setOnAction(e -> { authService.logout(); MainApp.showLoginScreen(); });
        header.getChildren().addAll(hLbl, sp, logoutBtn);
        root.setTop(header);

        // ---- CENTER ----
        VBox center = new VBox(18);
        center.getStyleClass().add("kidsbank-body");
        center.setPadding(new Insets(28));
        center.setAlignment(Pos.TOP_CENTER);

        Label pageTitle = MainApp.makeHeader("What would you like to do?");

        // Summary cards row
        HBox summaryRow = new HBox(16);
        summaryRow.setAlignment(Pos.CENTER);

        // Count children registered
        List<User> allUsers = JsonStorage.loadAllUsers();
        long childCount = allUsers.stream().filter(u -> "child".equals(u.getRole())).count();

        long pendingApprovals = taskService.getTasksForParent(parent.getUserId()).stream()
                .filter(t -> t.getStatus() == TaskStatus.COMPLETED).count();

        summaryRow.getChildren().addAll(
            summaryCard("👧 Children", String.valueOf(childCount), "#1A5276"),
            summaryCard("⏳ Pending Approvals", String.valueOf(pendingApprovals), "#D35400")
        );

        // Action buttons
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(14);
        grid.setAlignment(Pos.CENTER);

        Button createAccBtn = actionBtn("👶 Create Child Account", "#1A5276");
        Button depositBtn   = actionBtn("💰 Deposit Money",        "#1E8449");
        Button setTaskBtn   = actionBtn("📋 Set a Task",           "#6E2FA0");
        Button approveBtn   = actionBtn("✅ Approve Tasks",        "#D35400");
        Button historyBtn   = actionBtn("📜 View History",         "#1F4E79");
        Button registerBtn  = actionBtn("👤 Register Parent",      "#616A6B");
        Button childrenBtn  = actionBtn("👧 View Children",       "#2471A3");
        Button interestBtn  = actionBtn("💹 Apply Interest",       "#148F77");
        Button limitBtn     = actionBtn("🔒 Set Spending Limit",   "#935116");
        Button analyticsBtn = actionBtn("📊 Analytics",            "#6C3483");

        createAccBtn.setOnAction(e -> showCreateAccountDialog());
        depositBtn.setOnAction(e -> showDepositDialog());
        setTaskBtn.setOnAction(e -> MainApp.showTaskScreen(true));
        approveBtn.setOnAction(e -> MainApp.showTaskScreen(true));
        historyBtn.setOnAction(e -> showSelectAccountForHistory());
        analyticsBtn.setOnAction(e -> showSelectChildForAnalytics());
        registerBtn.setOnAction(e -> showRegisterParentDialog());
        childrenBtn.setOnAction(e -> MainApp.showChildProfiles());
        interestBtn.setOnAction(e -> showApplyInterestDialog());
        limitBtn.setOnAction(e -> showSpendingLimitDialog());

        grid.add(createAccBtn,   0, 0);
        grid.add(depositBtn,    1, 0);
        grid.add(setTaskBtn,    2, 0);
        grid.add(approveBtn,   0, 1);
        grid.add(historyBtn,   1, 1);
        grid.add(analyticsBtn, 2, 1);
        grid.add(registerBtn,  0, 2);
        grid.add(childrenBtn,  1, 2);
        grid.add(interestBtn,  2, 2);
        grid.add(limitBtn,     0, 3);
        GridPane.setColumnSpan(limitBtn, 3);

        center.getChildren().addAll(pageTitle, summaryRow, grid);
        root.setCenter(center);
    }

    private VBox summaryCard(String title, String value, String color) {
        VBox card = new VBox(4);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(14, 24, 14, 24));
        card.setStyle("-fx-background-color:white; -fx-background-radius:10; "
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.1),8,0,0,1);");
        Label valLbl = new Label(value);
        valLbl.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        valLbl.setTextFill(Color.web(color));
        Label titleLbl = new Label(title);
        titleLbl.setFont(Font.font("Arial", 11));
        titleLbl.setTextFill(Color.GRAY);
        card.getChildren().addAll(valLbl, titleLbl);
        return card;
    }

    private Button actionBtn(String text, String color) {
        Button btn = new Button(text);
        btn.setMinWidth(220);
        btn.setMinHeight(55);
        btn.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        btn.setStyle("-fx-background-color:" + color + "; -fx-text-fill:white; "
                + "-fx-background-radius:10; -fx-cursor:hand;");
        return btn;
    }

    // -------- DIALOGS --------

    private void showCreateAccountDialog() {
        Dialog<ButtonType> d = new Dialog<>();
        MainApp.styleDialogPane(d.getDialogPane());
        d.setTitle("Create Child Account");
        d.setHeaderText("Register a new child and create their bank account");

        TextField nameF = new TextField(); nameF.setPromptText("Child's name");
        TextField ageF  = new TextField(); ageF.setPromptText("Age (e.g. 10)");
        PasswordField pinF  = new PasswordField(); pinF.setPromptText("4-digit PIN");
        PasswordField pin2F = new PasswordField(); pin2F.setPromptText("Confirm PIN");

        ToggleGroup typeGroup = new ToggleGroup();
        RadioButton currentRb = new RadioButton("Current Account"); currentRb.setToggleGroup(typeGroup); currentRb.setSelected(true);
        RadioButton savingsRb = new RadioButton("Savings Account"); savingsRb.setToggleGroup(typeGroup);

        Label errLbl = new Label(""); errLbl.setTextFill(Color.RED);

        VBox content = new VBox(10,
            new Label("Child's Name:"), nameF,
            new Label("Age:"), ageF,
            new Label("Account Type:"), new HBox(16, currentRb, savingsRb),
            new Label("Set PIN:"), pinF,
            new Label("Confirm PIN:"), pin2F,
            errLbl
        );
        content.setPadding(new Insets(10));
        d.getDialogPane().setContent(content);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        d.showAndWait().ifPresent(bt -> {
            if (bt != ButtonType.OK) return;
            try {
                String name = nameF.getText().trim();
                int age = Integer.parseInt(ageF.getText().trim());
                String pin = pinF.getText().trim();
                String pin2 = pin2F.getText().trim();
                if (!pin.equals(pin2)) { MainApp.showError("PINs do not match."); return; }
                AccountType type = savingsRb.isSelected() ? AccountType.SAVINGS : AccountType.CURRENT;

                // Register user + create account
                User child = authService.registerUser(name, age, pin, "child");
                accountService.createAccount(child.getUserId(), type);
                MainApp.showInfo("Account Created",
                        "✅ Account created for " + name + "!\nThey can now login with their name and PIN.");
                MainApp.showParentDashboard();
            } catch (Exception ex) {
                MainApp.showError(ex.getMessage());
            }
        });
    }

    private void showDepositDialog() {
        // Pick a child first
        List<User> children = JsonStorage.loadAllUsers().stream()
                .filter(u -> "child".equals(u.getRole())).toList();
        if (children.isEmpty()) { MainApp.showError("No children registered yet."); return; }

        Dialog<ButtonType> d = new Dialog<>();
        MainApp.styleDialogPane(d.getDialogPane());
        d.setTitle("Deposit Money");
        d.setHeaderText("Deposit virtual money into a child's account");

        ComboBox<String> childCb = new ComboBox<>();
        children.forEach(c -> childCb.getItems().add(c.getName() + " [" + c.getUserId().substring(0,8) + "]"));
        childCb.getSelectionModel().selectFirst();

        TextField amtF = new TextField(); amtF.setPromptText("Amount (e.g. 10.00)");
        TextField descF = new TextField(); descF.setPromptText("Description (e.g. Weekly allowance)");

        VBox content = new VBox(10,
            new Label("Select Child:"), childCb,
            new Label("Amount ($):"), amtF,
            new Label("Description:"), descF
        );
        content.setPadding(new Insets(10));
        d.getDialogPane().setContent(content);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        d.showAndWait().ifPresent(bt -> {
            if (bt != ButtonType.OK) return;
            try {
                int idx = childCb.getSelectionModel().getSelectedIndex();
                User child = children.get(idx);
                List<Account> accs = accountService.getAccountsForUser(child.getUserId());
                if (accs.isEmpty()) { MainApp.showError("This child has no account yet."); return; }
                double amount = Double.parseDouble(amtF.getText().trim());
                String desc = descF.getText().trim().isEmpty() ? "Parent Deposit" : descF.getText().trim();
                accountService.deposit(accs.get(0).getAccountId(), amount, desc);
                MainApp.showInfo("Deposited!", "✅ $" + String.format("%.2f", amount)
                        + " added to " + child.getName() + "'s account.");
                MainApp.showParentDashboard();
            } catch (Exception ex) {
                MainApp.showError(ex.getMessage());
            }
        });
    }

    private void showSelectAccountForHistory() {
        List<User> kids = JsonStorage.loadAllUsers().stream()
                .filter(u -> "child".equals(u.getRole())).toList();
        if (kids.isEmpty()) { MainApp.showError("No children registered yet."); return; }

        ChoiceDialog<String> cd = new ChoiceDialog<>(kids.get(0).getName(),
                kids.stream().map(User::getName).toList());
        MainApp.styleDialogPane(cd.getDialogPane());
        cd.setTitle("View History");
        cd.setHeaderText("Select a child to view their transaction history");
        cd.showAndWait().ifPresent(name -> {
            kids.stream().filter(c -> c.getName().equals(name)).findFirst().ifPresent(child -> {
                List<Account> accs = accountService.getAccountsForUser(child.getUserId());
                if (accs.isEmpty()) { MainApp.showError("No account found."); return; }
                MainApp.showTransactionHistory(accs.get(0).getAccountId(),
                        child.getName() + "'s " + accs.get(0).getAccountType().getDisplayName());
            });
        });
    }

    private void showSelectChildForAnalytics() {
        List<User> kids = JsonStorage.loadAllUsers().stream()
                .filter(u -> "child".equals(u.getRole())).toList();
        if (kids.isEmpty()) {
            MainApp.showError("No children registered yet.");
            return;
        }

        ChoiceDialog<String> cd = new ChoiceDialog<>(kids.get(0).getName(),
                kids.stream().map(User::getName).toList());
        MainApp.styleDialogPane(cd.getDialogPane());
        cd.setTitle("Analytics");
        cd.setHeaderText("Select a child to view charts and summaries");
        cd.showAndWait().ifPresent(name ->
            kids.stream().filter(c -> c.getName().equals(name)).findFirst().ifPresent(child -> {
                List<Account> accs = accountService.getAccountsForUser(child.getUserId());
                if (accs.isEmpty()) {
                    MainApp.showError("No account found for this child.");
                    return;
                }
                MainApp.showAnalyticsForChild(accs.get(0).getAccountId(), child.getUserId());
            }));
    }

    private void showRegisterParentDialog() {
        Dialog<ButtonType> d = new Dialog<>();
        MainApp.styleDialogPane(d.getDialogPane());
        d.setTitle("Register Parent Account");
        d.setHeaderText("Create a new parent login");
        TextField nameF = new TextField(); nameF.setPromptText("Parent name");
        PasswordField pinF = new PasswordField(); pinF.setPromptText("4-digit PIN");
        PasswordField pin2F = new PasswordField(); pin2F.setPromptText("Confirm PIN");
        VBox content = new VBox(10, new Label("Name:"), nameF,
                new Label("PIN:"), pinF, new Label("Confirm PIN:"), pin2F);
        content.setPadding(new Insets(10));
        d.getDialogPane().setContent(content);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        d.showAndWait().ifPresent(bt -> {
            if (bt != ButtonType.OK) return;
            try {
                if (!pinF.getText().equals(pin2F.getText())) { MainApp.showError("PINs do not match."); return; }
                authService.registerUser(nameF.getText().trim(), 30, pinF.getText().trim(), "parent");
                MainApp.showInfo("Registered", "Parent account created successfully!");
            } catch (Exception ex) {
                MainApp.showError(ex.getMessage());
            }
        });
    }

    private void showApplyInterestDialog() {
        List<SavingsPick> picks = new ArrayList<>();
        for (User c : JsonStorage.loadAllUsers().stream()
                .filter(u -> "child".equals(u.getRole())).toList()) {
            for (Account a : accountService.getAccountsForUser(c.getUserId())) {
                if (a.getAccountType() == AccountType.SAVINGS) {
                    picks.add(new SavingsPick(c, a));
                }
            }
        }
        if (picks.isEmpty()) {
            MainApp.showError("No savings accounts found. Create a child with a savings account first.");
            return;
        }

        Dialog<ButtonType> d = new Dialog<>();
        MainApp.styleDialogPane(d.getDialogPane());
        d.setTitle("Apply Interest");
        d.setHeaderText("Add monthly interest to a child's savings balance");

        ComboBox<SavingsPick> cb = new ComboBox<>();
        cb.getItems().addAll(picks);
        cb.getSelectionModel().selectFirst();
        cb.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(SavingsPick object) {
                return object == null ? "" : object.label();
            }
            @Override public SavingsPick fromString(String string) { return null; }
        });

        TextField rateF = new TextField("2.0");
        rateF.setPromptText("Annual-style % for this allowance (e.g. 2.0)");

        VBox content = new VBox(10,
                new Label("Child / Savings account:"), cb,
                new Label("Interest rate (%):"), rateF);
        content.setPadding(new Insets(10));
        d.getDialogPane().setContent(content);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        d.showAndWait().ifPresent(bt -> {
            if (bt != ButtonType.OK) return;
            try {
                SavingsPick pick = cb.getSelectionModel().getSelectedItem();
                if (pick == null) return;
                double rate = Double.parseDouble(rateF.getText().trim());
                var tx = accountService.applyMonthlyInterest(pick.account().getAccountId(), rate);
                MainApp.showInfo("Interest Applied",
                        "Interest of $" + String.format("%.2f", tx.getAmount())
                                + " applied to " + pick.child().getName() + "'s savings account.");
                MainApp.showParentDashboard();
            } catch (Exception ex) {
                MainApp.showError(ex.getMessage());
            }
        });
    }

    private void showSpendingLimitDialog() {
        List<AcctPick> picks = new ArrayList<>();
        for (User c : JsonStorage.loadAllUsers().stream()
                .filter(u -> "child".equals(u.getRole())).toList()) {
            for (Account a : accountService.getAccountsForUser(c.getUserId())) {
                picks.add(new AcctPick(c, a));
            }
        }
        if (picks.isEmpty()) {
            MainApp.showError("No child accounts yet.");
            return;
        }

        Dialog<ButtonType> d = new Dialog<>();
        MainApp.styleDialogPane(d.getDialogPane());
        d.setTitle("Weekly Spending Limit");
        d.setHeaderText("Set the maximum withdrawals allowed per rolling week");

        ComboBox<AcctPick> cb = new ComboBox<>();
        cb.getItems().addAll(picks);
        cb.getSelectionModel().selectFirst();
        cb.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(AcctPick object) {
                return object == null ? "" : object.label();
            }
            @Override public AcctPick fromString(String string) { return null; }
        });

        TextField limitF = new TextField();
        limitF.setPromptText("Weekly limit ($), 0 = no limit");

        VBox content = new VBox(10,
                new Label("Account:"), cb,
                new Label("Weekly limit ($):"), limitF);
        content.setPadding(new Insets(10));
        d.getDialogPane().setContent(content);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        d.showAndWait().ifPresent(bt -> {
            if (bt != ButtonType.OK) return;
            try {
                AcctPick pick = cb.getSelectionModel().getSelectedItem();
                if (pick == null) return;
                double lim = Double.parseDouble(limitF.getText().trim());
                accountService.setSpendingLimitForAccount(pick.account().getAccountId(), lim);
                MainApp.showInfo("Limit Saved",
                        "Weekly spending limit set to $" + String.format("%.2f", lim)
                                + " for " + pick.child().getName() + ".");
                MainApp.showParentDashboard();
            } catch (Exception ex) {
                MainApp.showError(ex.getMessage());
            }
        });
    }

    private record SavingsPick(User child, Account account) {
        String label() {
            return child.getName() + " — " + account.getAccountType().getDisplayName()
                    + "  ($" + String.format("%.2f", account.getBalance()) + ")";
        }
    }

    private record AcctPick(User child, Account account) {
        String label() {
            return child.getName() + " — " + account.getAccountType().getDisplayName();
        }
    }

    public BorderPane getRoot() { return root; }
}
