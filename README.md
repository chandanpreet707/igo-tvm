# IGoApplication

IGoApplication is a JavaFX desktop application built with Spring Boot for managing TVM operations.

## Installation

### Prerequisites
* Java JDK 17+
* Maven
* JavaFX SDK 17+ (download from [Gluon](https://gluonhq.com/products/javafx/))
* Git

### Clone the repository
```bash
git clone https://github.com/chandanpreet707/igo-tvm.git
cd igo-tvm
```

### Build the project

```bash
mvn clean install
```

### Usage
#### Run with Maven
```bash
mvn javafx:run
```

#### or

#### Run from IDE (IntelliJ/Eclipse)
  1. Open the project as a Maven project.
  2. Add JavaFX SDK to your module dependencies.
  3. Set VM options (replace PATH_TO_FX with your JavaFX SDK path):

```bash
--module-path PATH_TO_FX --add-modules javafx.controls,javafx.fxml
```
  4. Run the main class:
```java
concordia.soen6611.igo_tvm.IGoApplication
```
#### Run from command line
```bash
java --module-path PATH_TO_FX --add-modules javafx.controls,javafx.fxml -jar target/igo-application-1.0-SNAPSHOT.jar
```

## Contributing

Pull requests are welcome. For major changes, please open an issue first
to discuss what you would like to change.

Please make sure to update tests and VM configurations as appropriate.

Thanks & Regards
