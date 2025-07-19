# ActiveMQ/Camel Example

This repository contains a small demo showing how Apache Camel can be used together with an embedded ActiveMQ broker. Orders are read from files or JMS, sent through billing and inventory validation, and the results are written to separate queues.

## Prerequisites

* Java Development Kit (JDK) 8 or newer
* `javac` and `java` on your `PATH`

The required libraries are already provided under `Example_ActiveMQCamel/Example_libs`.

## Building

Compile all Java sources and place the classes under `build/classes`:

```bash
mkdir -p build/classes
javac -cp "Example_ActiveMQCamel/Example_libs/*" \
  -d build/classes Example_ActiveMQCamel/*/src/de/tuberlin/cit/vs/*.java
```

(Replace the classpath separator `:` with `;` when running on Windows.)

## Running

Each component is a separate Java program. Start them in separate terminals after compiling:

1. **IntegrationApp** – starts Camel and an embedded ActiveMQ broker:
   ```bash
   java -cp "build/classes:Example_ActiveMQCamel/Example_libs/*" \
     de.tuberlin.cit.vs.IntegrationApp
   ```

2. **BillingSystem** – validates customers:
   ```bash
   java -cp "build/classes:Example_ActiveMQCamel/Example_libs/*" \
     de.tuberlin.cit.vs.BillingSystem
   ```

3. **InventorySystem** – checks available stock:
   ```bash
   java -cp "build/classes:Example_ActiveMQCamel/Example_libs/*" \
     de.tuberlin.cit.vs.InventorySystem
   ```

4. **ResultSystem** – prints the final valid/invalid orders:
   ```bash
   java -cp "build/classes:Example_ActiveMQCamel/Example_libs/*" \
     de.tuberlin.cit.vs.ResultSystem
   ```

Optionally start **WebOrderSystem** to enter orders interactively or run **CallCenterOrderSystem** to simulate batch files in `orders/`.

Press `<enter>` in each program to stop it.
