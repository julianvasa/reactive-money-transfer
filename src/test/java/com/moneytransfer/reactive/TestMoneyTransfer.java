package com.moneytransfer.reactive;

import com.jayway.restassured.RestAssured;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.*;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.ServerSocket;

import static com.jayway.restassured.RestAssured.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@RunWith(VertxUnitRunner.class)
public class TestMoneyTransfer {

    private Vertx vertx;

    @Before
    public void setUp(TestContext context) throws IOException {
        vertx = Vertx.vertx();
        ServerSocket socket = new ServerSocket(8080);
        port = socket.getLocalPort();
        socket.close();
        DeploymentOptions options = new DeploymentOptions()
            .setConfig(new JsonObject().put("http.port", port)
            );
        vertx.deployVerticle(MainVerticle.class.getName(), options, context.asyncAssertSuccess());
    }


    @BeforeClass
    public static void setupRestAssured() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 8080;
    }

    @Test
    public void healthCheck(TestContext context) {
        String healthCheck = get("/health").asString();
        assertThat(healthCheck).isEqualTo("OK");
    }

    @After
    public void tearDown(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    @AfterClass
    public static void resetRestAssured() {
        RestAssured.reset();
    }

    @Test
    public void getAllAccounts() {
        final int id = get("/accounts").then()
            .assertThat()
            .statusCode(200)
            .extract()
            .jsonPath().getInt("find { it.balance==300 }.id");
        get("/accounts/" + id).then()
            .assertThat()
            .statusCode(200)
            .body("id", equalTo(id))
            .body("name", equalTo("account 3"))
            .body("currency", equalTo("GBP"));
    }

    @Test
    public void getOneAccount() {
        get("/accounts/1111").then()
            .assertThat()
            .statusCode(200)
            .body("id", equalTo(1111))
            .body("balance", equalTo(100))
            .body("name", equalTo("account 1"))
            .body("currency", equalTo("EUR"));
    }

    @Test
    public void getOneAccountThatDoesNotExist() {
        get("/accounts/1000").then()
            .assertThat()
            .statusCode(404);
    }

    @Test
    public void accountDeposit_AccountNumbernDoesNotExist() {
        put("/accounts/2233224/deposit/1000").then()
            .assertThat()
            .statusCode(404);
    }

    @Test
    public void accountDeposit() {
        put("/accounts/1111/deposit/1000").then()
            .assertThat()
            .statusCode(200);
    }

    @Test
    public void accountWithdraw_InsufficientFunds() {
        put("/accounts/1111/withdraw/10000000").then()
            .assertThat()
            .statusCode(403);
    }

    @Test
    public void accountWithdraw() {
        put("/accounts/1111/withdraw/10").then()
            .assertThat()
            .statusCode(200);
    }

    @Test
    public void accountOperationOtherThanDepositOrWithdrawNotAllowed() {
        put("/accounts/1111/paySomeOne/10").then()
            .assertThat()
            .statusCode(404);
    }


    @Test
    public void addNewAccount() {
        given().body("{\n" +
            "  \"id\": 444,\n" +
            "  \"name\": \"Julian Vasa\",\n" +
            "  \"balance\": 500.2,\n" +
            "  \"currency\": \"EUR\"\n" +
            "}")
            .when()
            .post("/accounts")
            .then()
            .assertThat()
            .statusCode(201);
    }

    @Test
    public void getAccountWithNonNumericAccountNumber() {
        get("/accounts/asd").then()
            .assertThat()
            .statusCode(400);
    }

    @Test
    public void addNewAccount_AccountNumberAlreadyExists() {
        given().body("{\n" +
            "  \"id\": 1111,\n" +
            "  \"name\": \"Julian Vasa\",\n" +
            "  \"balance\": 500.2,\n" +
            "  \"currency\": \"EUR\"\n" +
            "}")
            .when()
            .post("/accounts")
            .then()
            .assertThat()
            .statusCode(409);
    }

    @Test
    public void addNewAccountWithIncorrectJSON() {
        given().body("{\n" +
            "    \"name\": \"Julian Vasa\",\n" +
            "    \"balance\": \"12355\",\n" +
            "    \"currency\": \"aaaa\"\n" +
            "}")
            .when()
            .post("/accounts")
            .then()
            .assertThat()
            .statusCode(415);
    }

    @Test
    public void deleteOneAccount() {
        delete("/accounts/1111").then()
            .assertThat()
            .statusCode(204);
        get("/accounts/1111").then()
            .assertThat()
            .statusCode(404);
    }

    @Test
    public void deleteOneAccountThatDoesNotExist() {
        delete("/accounts/999").then()
            .assertThat()
            .statusCode(404);
    }

    @Test
    public void getAllTransactions_PickOneAndGetTransactionDetails() {
        final int id = get("/transactions").then()
            .assertThat()
            .body("size()", is(2))
            .statusCode(200)
            .extract()
            .jsonPath().getInt("find { it.amount==34 }.id");

        get("/transactions/" + id).then()
            .assertThat()
            .statusCode(200)
            .body("id", equalTo(id))
            .body("fromAccount", equalTo(3333))
            .body("toAccount", equalTo(1111))
            .body("amount", equalTo(34))
            .body("currency", equalTo("USD"))
            .body("description", equalTo("test transaction 2"));
    }

    @Test
    public void getTransactionOfOneAccount() {
        get("/transactions/account/1111").then()
            .assertThat()
            .body("size()", is(2))
            .statusCode(200)
            .body("amount", hasItems(12, 34));
    }

    @Test
    public void getTransactionOfOneAccountThatDoesNotExist() {
        get("/transactions/account/111111111").then()
            .assertThat()
            .statusCode(404);
    }

    @Test
    public void getAllTransactions() {
        get("/transactions").then()
            .assertThat()
            .statusCode(200)
            .body("amount", hasItems(12, 34))
            .body("description", hasItems("test transaction 1", "test transaction 2"))
            .body("currency", hasItems("EUR", "USD"));
    }

    @Test
    public void getTransactionThatDoesNotExist() {
        get("/transactions/2").then()
            .assertThat()
            .statusCode(404);
    }

    @Test
    public void newTransaction() {
        given().body("{\n" +
            "    \"fromAccount\": \"2222\",\n" +
            "    \"toAccount\": \"1111\",\n" +
            "    \"amount\": \"14.4\",\n" +
            "    \"currency\": \"USD\",\n" +
            "    \"description\": \"test transfer\"\n" +
            "}")
            .when()
            .post("/transactions")
            .then()
            .assertThat()
            .statusCode(201);
    }

    @Test
    public void newTransactionWithIncorrectJSON() {
        given().body("{\n" +
            "    \"fromAccount\": \"2222\",\n" +
            "    \"toAccount\": \"1111\",\n" +
            "    \"amount\": \"1000\",\n" +
            "    \"currency\": \"USWD\",\n" +
            "    \"description\": \"test transfer\"\n" +
            "}")
            .when()
            .post("/transactions")
            .then()
            .assertThat()
            .statusCode(415);
    }

    @Test
    public void newTransactionWithNonNumericAccount() {
        given().body("{\n" +
            "    \"fromAccount\": \"asaadaasdasdasd\",\n" +
            "    \"toAccount\": \"1111\",\n" +
            "    \"amount\": \"1000\",\n" +
            "    \"currency\": \"USWD\",\n" +
            "    \"description\": \"test transfer\"\n" +
            "}")
            .when()
            .post("/transactions")
            .then()
            .assertThat()
            .statusCode(415);
    }

    @Test
    public void newTransactionWithIdAlreadyPresentInTheDB() {
        final int id = get("/transactions").then()
            .assertThat()
            .statusCode(200)
            .extract()
            .jsonPath().getInt("find { it.amount==34 }.id");

        given().body("{\n" +
            "    \"id\": \"" + id + "\",\n" +
            "    \"fromAccount\": \"2222\",\n" +
            "    \"toAccount\": \"1111\",\n" +
            "    \"amount\": \"14.4\",\n" +
            "    \"currency\": \"USD\",\n" +
            "    \"description\": \"test transfer\"\n" +
            "}")
            .when()
            .post("/transactions")
            .then()
            .assertThat()
            .statusCode(409);
    }

    @Test
    public void newTransactionWithSourceAccountThatDoesNotExist() {
        given().body("{\n" +
            "    \"fromAccount\": \"46667888\",\n" +
            "    \"toAccount\": \"1111\",\n" +
            "    \"amount\": \"14.4\",\n" +
            "    \"currency\": \"USD\",\n" +
            "    \"description\": \"test transfer\"\n" +
            "}")
            .when()
            .post("/transactions")
            .then()
            .assertThat()
            .statusCode(404);
    }

    @Test
    public void newTransactionWithDestinationAccountThatDoesNotExist() {
        given().body("{\n" +
            "    \"fromAccount\": \"2222\",\n" +
            "    \"toAccount\": \"1231231312\",\n" +
            "    \"amount\": \"14.4\",\n" +
            "    \"currency\": \"USD\",\n" +
            "    \"description\": \"test transfer\"\n" +
            "}")
            .when()
            .post("/transactions")
            .then()
            .assertThat()
            .statusCode(404);
    }

    @Test
    public void newTransactionWithoutTransactionAmount() {
        given().body("{\n" +
            "    \"fromAccount\": \"2222\",\n" +
            "    \"toAccount\": \"1111\",\n" +
            "    \"currency\": \"USD\",\n" +
            "    \"description\": \"test transfer\"\n" +
            "}")
            .when()
            .post("/transactions")
            .then()
            .assertThat()
            .statusCode(409);
    }

    @Test
    public void newTransactionWithNegativeTransactionAmount() {
        given().body("{\n" +
            "    \"fromAccount\": \"2222\",\n" +
            "    \"toAccount\": \"1111\",\n" +
            "    \"amount\": \"-14.4\",\n" +
            "    \"currency\": \"USD\",\n" +
            "    \"description\": \"test transfer\"\n" +
            "}")
            .when()
            .post("/transactions")
            .then()
            .assertThat()
            .statusCode(409);
    }

    @Test
    public void newTransaction_InsufficientFunds() {
        given().body("{\n" +
            "    \"fromAccount\": \"2222\",\n" +
            "    \"toAccount\": \"1111\",\n" +
            "    \"amount\": \"1000000000\",\n" +
            "    \"currency\": \"USD\",\n" +
            "    \"description\": \"test transfer\"\n" +
            "}")
            .when()
            .post("/transactions")
            .then()
            .assertThat()
            .statusCode(409);
    }


}
