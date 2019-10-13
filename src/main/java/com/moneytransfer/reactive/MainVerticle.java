package com.moneytransfer.reactive;

import com.moneytransfer.reactive.enums.AccountOperation;
import com.moneytransfer.reactive.enums.TransactionStatus;
import com.moneytransfer.reactive.model.Account;
import com.moneytransfer.reactive.model.Transaction;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.moneytransfer.reactive.exception.Exception.error;

/**
 * MainVerticle
 */
public class MainVerticle extends AbstractVerticle {
    private final Map<Integer, Account> accounts = new LinkedHashMap<>();
    private final Map<Integer, Transaction> transactions = new LinkedHashMap<>();
    private final String JSON_CONTENT_TYPE = "application/json; charset=utf-8";

    @Override
    public void start(Promise<Void> startPromise) {
        insertSampleData();

        /* Endpoints are exposed through a Router that will map a route to a handler which is basically the business code */
        Router router = Router.router(vertx);
        /* Enables the reading of the request body for all routes under /accounts */
        router.route("/accounts").handler(BodyHandler.create());
        /* Enables the reading of the request body for all routes under /transactions */
        router.route("/transactions").handler(BodyHandler.create());

        /* Validate account number (id) before proceeding with any other endpoint which needs an id as a parameter */
        router.route("/accounts/:id").handler(this::validateAccountNumber);
        /* Get all accounts */
        router.get("/accounts").handler(this::getAllAccounts);
        /* Post a new account */
        router.post("/accounts").handler(this::newAccount);
        /* Get account by Id */
        router.get("/accounts/:id").handler(this::getAccount);
        /* Delete an account */
        router.delete("/accounts/:id").handler(this::deleteAccount);
        /* Deposit or Withdraw */
        router.put("/accounts/:id/deposit/:amount").handler(
            routingContext -> this.accountOperation(routingContext, AccountOperation.DEPOSIT));
        router.put("/accounts/:id/withdraw/:amount").handler(
            routingContext -> this.accountOperation(routingContext, AccountOperation.WITHDRAW));

        /* Get all transactions */
        router.get("/transactions").handler(this::getAllTransactions);
        /* Post a new transaction */
        router.post("/transactions").handler(this::newTransaction);
        /* Get transaction by Id */
        router.get("/transactions/:id").handler(this::getTransaction);
        /* Get all transactions of a certain account identified with the provided Id */
        router.get("/transactions/account/:id").handler(this::getTransactionOfAccount);

        /* Just a simple endpoint to check whether the server is responding or not */
        router.get("/health").handler(rc -> rc.response().end("OK"));
        /* Start the HTTP server on port 8080 */
        vertx.createHttpServer()
            .requestHandler(router)
            .listen(
                8080,
                result -> {
                    if (result.succeeded()) {
                        startPromise.complete();
                    }
                    else {
                        startPromise.fail(result.cause());
                    }
                }
            );
    }

    /* Validate account number (id) before proceeding with any other endpoint which needs an id as a parameter */
    private void validateAccountNumber(RoutingContext routingContext) {
        try {
            Integer.parseInt(routingContext.pathParam("id"));
            routingContext.next();
        } catch (NumberFormatException exception) {
            error(routingContext, 400, "Invalid Account Number: " + exception.getCause());
        }
    }

    /* Get all accounts */
    private void getAllAccounts(RoutingContext routingContext) {
        routingContext.response()
            .putHeader(HttpHeaders.CONTENT_TYPE, JSON_CONTENT_TYPE)
            .setStatusCode(200)
            .end(Json.encodePrettily(accounts.values()));
    }

    /* Get account by Id */
    private void getAccount(RoutingContext routingContext) {
        final String id = routingContext.request().getParam("id");
        final Integer accountNumber = Integer.valueOf(id);
        Account account = accounts.get(accountNumber);
        if (account == null) {
            error(routingContext, 404, "Account Number not found in the DB: " + id);
        }
        else {
            sendAccountResponse(routingContext, account, 200);
        }
    }

    /* Post a new account */
    private void newAccount(RoutingContext routingContext) {
        try {
            final Account account = Json.decodeValue(routingContext.getBodyAsString(), Account.class);

            if (accounts.containsKey(account.getId())) {
                error(routingContext, 409, "Account number already exists in the DB!");
            }
            accounts.put(account.getId(), account);
            sendAccountResponse(routingContext, account, 201);
        } catch (RuntimeException exception) {
            error(routingContext, 415, "Unable to parse Account JSON request body! Cause: " + exception.getCause());
        }
    }

    /* Delete an account */
    private void deleteAccount(RoutingContext routingContext) {
        String id = routingContext.request().getParam("id");
        if (accounts.get(Integer.valueOf(id)) == null) {
            error(routingContext, 404, "Account Number not found in the DB: " + id);
        }
        else {
            Integer accountNumber = Integer.valueOf(id);
            accounts.remove(accountNumber);
            error(routingContext, 204, "Account deleted: " + accountNumber);
        }
    }

    /* Deposit or Withdraw */
    private void accountOperation(RoutingContext routingContext, AccountOperation operation) {
        final String id = routingContext.pathParam("id");
        final String amountParam = routingContext.pathParam("amount");
        final Integer accountNumber = Integer.valueOf(id);
        final BigDecimal amount = BigDecimal.valueOf(Long.parseLong(amountParam));

        if (accounts.get(accountNumber) == null) {
            error(routingContext, 404, "Account Number not found in the DB: " + id);
        }
        else {
            Account account = accounts.get(accountNumber);
            if (operation.equals(AccountOperation.DEPOSIT)) {
                account.deposit(amount);
                sendAccountResponse(routingContext, account, 200);
            }
            else if (operation.equals(AccountOperation.WITHDRAW)) {
                if (account.getBalance().compareTo(amount) < 0) {
                    error(routingContext, 403, "Account balance < amount: s" + amount);
                }
                account.withdraw(amount);
                sendAccountResponse(routingContext, account, 200);
            }
        }
    }


    /* Get the transactions for a certain account (source or destination account) */
    private void getTransactionOfAccount(RoutingContext routingContext) {
        final String id = routingContext.request().getParam("id");
        final Integer accountNumber = Integer.parseInt(id);
        Account fromAccount = accounts.get(accountNumber);
        if (fromAccount == null) {
            error(routingContext, 404, "Source Account does not exist!");
        }
        else {
            Predicate<Transaction> isSourceAccount = e -> e.getFromAccount() == accountNumber;
            Predicate<Transaction> isDestinationAccount = e -> e.getToAccount() == accountNumber;

            List<Transaction> transactionList = transactions
                .values()
                .stream()
                .filter(isSourceAccount.or(isDestinationAccount))
                .collect(Collectors.toUnmodifiableList());

            routingContext.response()
                .putHeader(HttpHeaders.CONTENT_TYPE, JSON_CONTENT_TYPE)
                .setStatusCode(200)
                .end(Json.encodePrettily(transactionList));
        }
    }

    /* Get transaction by Id */
    private void getTransaction(RoutingContext routingContext) {
        final String id = routingContext.request().getParam("id");
        final Integer transactionId = Integer.valueOf(id);
        Transaction transaction = transactions.get(transactionId);
        if (transaction == null) {
            error(routingContext, 404, "Transaction not found in the DB: " + id);
        }
        else {
            sendTransactionResponse(routingContext, transaction, 200);
        }
    }

    /* Post a new transaction */
    private void newTransaction(RoutingContext routingContext) {
        try {
            final Transaction transaction = Json.decodeValue(routingContext.getBodyAsString(), Transaction.class);
            if (transactions.containsKey(transaction.getId())) {
                error(routingContext, 409, "Transaction already exists in the DB!");
            }

            Account fromAccount = accounts.get(transaction.getFromAccount());
            if (fromAccount == null) {
                error(routingContext, 404, "Source Account does not exist!");
            }
            Account toAccount = accounts.get(transaction.getToAccount());
            if (toAccount == null) {
                error(routingContext, 404, "Destination Account does not exist!");
            }
            BigDecimal amount = transaction.getAmount();
            if (amount == null || (amount !=null  && amount.compareTo(BigDecimal.ZERO) <0)) {
                error(routingContext, 409, "Incorrenct transaction amount!");
            }

            if(fromAccount != null && toAccount != null && amount.compareTo(BigDecimal.ZERO)>0) {
                if (fromAccount.getBalance().compareTo(amount) < 0) {
                    error(routingContext, 409, "Insufficient funds! Unable to process the transfer!");
                }

                fromAccount.withdraw(amount);
                toAccount.deposit(amount);
                transaction.setStatus(TransactionStatus.SUCCESSFUL);
                transactions.put(transaction.getId(), transaction);
                sendTransactionResponse(routingContext, transaction, 201);
            }
        } catch (RuntimeException exception) {
            error(routingContext, 415, "Unable to parse Transaction JSON request body! Cause: " + exception.getCause());
        }
    }

    /* Get all transactions */
    private void getAllTransactions(RoutingContext routingContext) {
        routingContext.response()
            .putHeader(HttpHeaders.CONTENT_TYPE, JSON_CONTENT_TYPE)
            .setStatusCode(200)
            .end(Json.encodePrettily(transactions.values()));
    }

    /* Send transaction details to the client as a HttpServerResponse */
    private void sendTransactionResponse(RoutingContext routingContext, Transaction transaction, int statusCode) {
        routingContext.response()
            .putHeader(HttpHeaders.CONTENT_TYPE, JSON_CONTENT_TYPE)
            .setStatusCode(statusCode)
            .end(Json.encodePrettily(transaction));
    }

    /* Send account details to the client as a HttpServerResponse */
    private void sendAccountResponse(RoutingContext routingContext, Account account, int statusCode) {
        routingContext.response()
            .putHeader(HttpHeaders.CONTENT_TYPE, JSON_CONTENT_TYPE)
            .setStatusCode(statusCode)
            .end(Json.encodePrettily(account));
    }

    /* Insert some sample data on server start */
    private void insertSampleData() {
        Account account1 = Account.builder()
            .id(1111)
            .name("account 1")
            .balance(BigDecimal.valueOf(100))
            .currency(Currency.getInstance("EUR"))
            .build();
        Account account2 = Account.builder()
            .id(2222)
            .name("account 2")
            .balance(BigDecimal.valueOf(200))
            .currency(Currency.getInstance("USD"))
            .build();
        Account account3 = Account.builder()
            .id(3333)
            .name("account 3")
            .balance(BigDecimal.valueOf(300))
            .currency(Currency.getInstance("GBP"))
            .build();
        accounts.put(1111, account1);
        accounts.put(2222, account2);
        accounts.put(3333, account3);

        Transaction transaction1 = new Transaction(2222, 1111, BigDecimal.valueOf(12), Currency.getInstance("EUR"));
        Transaction transaction2 = new Transaction(3333, 1111, BigDecimal.valueOf(34), Currency.getInstance("USD"));
        transaction1.setStatus(TransactionStatus.SUCCESSFUL);
        transaction2.setStatus(TransactionStatus.SUCCESSFUL);
        transaction1.setDescription("test transaction 1");
        transaction2.setDescription("test transaction 2");
        transactions.put(transaction1.getId(), transaction1);
        transactions.put(transaction2.getId(), transaction2);
    }
}
