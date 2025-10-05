iGoApplication
iGoApplication is a JavaFX desktop application built with Spring Boot. This project uses Maven for dependency management and JavaFX for the user interface.

Prerequisites

Java JDK 17+
Download and install from Adoptium
 or your preferred JDK provider.

Maven
Make sure Maven is installed and added to your PATH.

mvn -v


JavaFX SDK
Download JavaFX from Gluon
. Extract the folder and note the path (used in VM options).

Git
For cloning and version control.

git --version

Clone the Repository
git clone https://github.com/USERNAME/REPO.git
cd REPO

Build the Project

Use Maven to build the project:

mvn clean install


This will download dependencies and compile the source code.

Run the Application (IDE)

If you are using IntelliJ IDEA or Eclipse:

Open the project as a Maven project.

Make sure JavaFX libraries are added to the module dependencies.

Set VM options for JavaFX (replace PATH_TO_FX with your JavaFX SDK path):

--module-path PATH_TO_FX --add-modules javafx.controls,javafx.fxml


Run IGoApplication (class: concordia.soen6611.igo_tvm.IGoApplication).

Run the Application (Command Line)
mvn javafx:run


Or manually using java:

java --module-path PATH_TO_FX --add-modules javafx.controls,javafx.fxml -jar target/igo-application-1.0-SNAPSHOT.jar

VM Arguments (Optional)

If running from IDE or Maven, you can set:

Module Path for JavaFX: --module-path PATH_TO_FX

Modules to add: --add-modules javafx.controls,javafx.fxml

Spring Boot active profile (optional): -Dspring.profiles.active=dev

Project Structure
src/main/java
  └── concordia/soen6611/igo_tvm/
        ├── IGoApplication.java
        ├── controller/
        ├── model/
        └── service/
src/main/resources
  ├── application.properties
  └── fxml/

Dependencies

JavaFX 17+

Spring Boot 3+

Maven

All dependencies are managed via Maven.

Common Issues

ClassNotFoundException for IGoApplication

Ensure package name matches: concordia.soen6611.igo_tvm

Make sure Maven has compiled the classes:

mvn clean compile


JavaFX not found

Check that --module-path points to the correct JavaFX SDK.

Contributing

Fork the repository.

Create a branch for your feature:

git checkout -b feature/your-feature


Commit your changes:

git commit -m "Add some feature"


Push to your branch:

git push origin feature/your-feature


Open a pull request.
