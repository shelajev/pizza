package io.diagrid.dapr;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.domain.CloudEvent;
import io.dapr.client.domain.State;

import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.EvaluationContext;
import dev.openfeature.sdk.EventDetails;
import dev.openfeature.sdk.FlagEvaluationDetails;
import dev.openfeature.sdk.MutableContext;
import dev.openfeature.sdk.OpenFeatureAPI;


@SpringBootApplication
@RestController
@CrossOrigin(origins = "http://localhost:5173", maxAge = 3600)
public class PizzaStore {

  @Value("${DAPR_HTTP_ENDPOINT:http://localhost:3500}")
  private String daprHttp;

  @Value("${STATE_STORE_NAME:kvstore}")
  private String STATE_STORE_NAME;
  
  @Value("${PUBLIC_IP:localhost}")
  private String publicIp;

  public boolean v2_enabled;
  public String backgroundColor =  v2_enabled ? "MediumSeaGreen" : "Gold";

  private final OpenFeatureAPI openFeatureAPI;

  @GetMapping("/server-info")
  public Info getInfo(){
    v2_enabled = getFeatureFlagValue();
    backgroundColor =  v2_enabled ? "MediumSeaGreen" : "Gold";
    System.out.println("Changing color to " + backgroundColor);
    return new Info(publicIp, backgroundColor);
  }

  public record Info(String publicIp, String backgroundColor){}

  private String KEY = "orders";
  private static RestTemplate restTemplate;

  private final SimpMessagingTemplate simpMessagingTemplate;

  public static void main(String[] args) {
    SpringApplication.run(PizzaStore.class, args);
  }

  @Autowired
  public PizzaStore(SimpMessagingTemplate simpMessagingTemplate, OpenFeatureAPI OFApi) {
    this.simpMessagingTemplate = simpMessagingTemplate;
    this.openFeatureAPI = OFApi;
  }

  // If an event comes in saying the order ready from the kitchen service then prepare the order for delivery
  @PostMapping(path = "/events", consumes = "application/cloudevents+json")
  public void receiveEvents(@RequestBody CloudEvent<Event> event) {
    emitWSEvent(event.getData());
    System.out.println("Received CloudEvent via Subscription: " + event.toString());
    Event pizzaEvent = event.getData();
    if(pizzaEvent.type.equals(EventType.ORDER_READY)){
      prepareOrderForDelivery(pizzaEvent.order);
    }
  }

  private void emitWSEvent(Event event) {
    System.out.println("Emitting Event via WS: " + event.toString());
    simpMessagingTemplate.convertAndSend("/topic/events",
        event);
  }

  // Notify the customer that the order is going out for delivery and call the delivery service
  private void prepareOrderForDelivery(Order order){
    store(new Order(order.id, order.customer, order.items, order.orderDate, Status.delivery));
     // Emit Event
    Event event = new Event(EventType.ORDER_OUT_FOR_DELIVERY, order, "store", "Delivery in progress.");
    emitWSEvent(event);

    callDeliveryService(order);

  }

  // A new order was received
  @PostMapping("/order")
  public ResponseEntity<Order> placeOrder(@RequestBody(required = true) Order order, Map<String, String> headers) throws Exception {
    new Thread(new Runnable() {
      @Override
      public void run() {
        // Emit Event
        Event event = new Event(EventType.ORDER_PLACED, order, "store", "We received the payment your order is confirmed.");

        emitWSEvent(event);
        // Store Order
        store(order);

        // Process Order, sent to kitcken
        callKitchenService(order);
      }
    }).start();

    return ResponseEntity.ok(order);

  }

  // Get the last order
  @GetMapping("/order")
  public ResponseEntity<Orders> getOrders() {

    Orders orders = loadOrders();

    return ResponseEntity.ok(orders);
  }

  public record Customer(@JsonProperty String name, @JsonProperty String email) {
  }

  public record OrderItem(@JsonProperty PizzaType type, @JsonProperty int amount) {
  }

  public enum PizzaType {
    pepperoni, margherita, hawaiian, vegetarian, kubernetescheese, daprcheese, clustertomatoes, diagridpepperoni, distributedolives, opensauce, workflowspread, plantbasedobservability, bindingsbacon
  }

  public enum Status {
    created, placed, notplaced, instock, notinstock, inpreparation, delivery, completed, failed
  }

  public record Event(EventType type, Order order, String service, String message) {
  }

  public enum EventType {

    ORDER_PLACED("order-placed"),
    ITEMS_IN_STOCK("items-in-stock"),
    ITEMS_NOT_IN_STOCK("items-not-in-stock"),
    ORDER_IN_PREPARATION("order-in-preparation"),
    ORDER_READY("order-ready"),
    ORDER_OUT_FOR_DELIVERY("order-out-for-delivery"),
    ORDER_ON_ITS_WAY("order-on-its-way"),
    ORDER_COMPLETED("order-completed");

    private String type;

    EventType(String type) {
      this.type = type;
    }

    @JsonValue
    public String getType() {
      return type;
    }
  }

  public record KitchenResponse(@JsonProperty String message, @JsonProperty String orderId) {
  }

  private record Orders(@JsonProperty List<Order> orders) {
  }

  public record Order(@JsonProperty String id, @JsonProperty Customer customer, @JsonProperty List<OrderItem> items,
      @JsonProperty Date orderDate, @JsonProperty Status status) {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Order(String id, Customer customer, List<OrderItem> items, Date orderDate, Status status) {
      if (id == null) {
        this.id = UUID.randomUUID().toString();
      } else {
        this.id = id;
      }
      this.customer = customer;
      this.items = items;
      if (orderDate == null) {
        this.orderDate = new Date();
      } else {
        this.orderDate = orderDate;
      }
      if (status == null) {
        this.status = Status.created;
      } else {
        this.status = status;
      }
    }

    public Order(Customer customer, List<OrderItem> items, Date orderDate, Status status) {
      this(UUID.randomUUID().toString(), customer, items, orderDate, status);
    }

    public Order(Customer customer, List<OrderItem> items) {
      this(UUID.randomUUID().toString(), customer, items, new Date(), Status.created);
    }

    public Order(Order order) {
      this(order.id, order.customer, order.items, order.orderDate, order.status);
    }
  }

  // Save order in state store
  private void store(Order order) {
    try (DaprClient client = (new DaprClientBuilder()).build()) {
      Orders orders = new Orders(new ArrayList<Order>());
      State<Orders> ordersState = client.getState(STATE_STORE_NAME, KEY, null, Orders.class).block();
      if (ordersState.getValue() != null && ordersState.getValue().orders.isEmpty()) {
        orders.orders.addAll(ordersState.getValue().orders);
      }
      orders.orders.add(order);
      // Save state
      client.saveState(STATE_STORE_NAME, KEY, orders).block();

    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  // Call kitchen service to make the order
  private void callKitchenService(Order order) {
    restTemplate = new RestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.add("Content-Type", "application/json");
    headers.add("dapr-app-id", "kitchen-service");
    HttpEntity<Order> request = new HttpEntity<Order>(order, headers);
    System.out.println("Calling Kitchen service at: " + daprHttp + "/prepare");
    restTemplate.put(
        daprHttp + "/prepare", request);
  }

  // Call delivery service to deliver the order
  private void callDeliveryService(Order order) {
    restTemplate = new RestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.add("Content-Type", "application/json");
    headers.add("dapr-app-id", "delivery-service");
    HttpEntity<Order> request = new HttpEntity<Order>(order, headers);
    System.out.println("Calling Delivery service at: " + daprHttp + "/deliver");
    restTemplate.put(
        daprHttp + "/deliver", request);
  }

  // Return the most recent order
  private Orders loadOrders() {
    try (DaprClient client = (new DaprClientBuilder()).build()) {
      State<Orders> ordersState = client.getState(STATE_STORE_NAME, KEY, null, Orders.class).block();
      return ordersState.getValue();

    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return null;

  }

  public boolean getFeatureFlagValue(){
    final Client client = openFeatureAPI.getClient();

    client.onProviderError((EventDetails eventDetails) -> {
      System.out.println("FlagD Provider has thrown an Error: " + eventDetails);
    });

    client.onProviderReady((EventDetails eventDetails)-> {
      System.out.println("FlagD Provider is Ready! " + eventDetails);
    });

    // get a bool flag value
    Boolean flagValue = client.getBooleanValue("v2_enabled", false);
    FlagEvaluationDetails<Boolean> flagValue1 = client.getBooleanDetails("v2_enabled", false);

    System.out.println("Feature flag returned is: " + flagValue);
    System.out.println("Boolean FlagEvaluationDetails is: " + flagValue1);

    return flagValue;
  }
}
