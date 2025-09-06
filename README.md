Inventory Management System 📦
A Java-based inventory management application built with Spring Boot, using MySQL for data persistence and Postman for API testing. This project provides a robust RESTful API for managing inventory items efficiently.
Features ✨

RESTful CRUD API: Create, Read, Update, and Delete operations for inventory items using Spring Boot.
MySQL Integration: Persistent storage and management of inventory data.
API Testing: Validated and tested endpoints using Postman.
Structured Project: Built with Maven, including pom.xml and .mvn wrapper for consistent builds.
Version Control: Includes .gitignore and .gitattributes for streamlined repository management.

Tech Stack 🛠️



Technology
Description
Badge



Java
Core programming language (Java 11 or higher)



Spring Boot
Framework for building RESTful APIs



MySQL
Database for storing inventory data



Postman
Tool for API testing and validation



Maven
Dependency management and build tool



Project Setup ⚙️

Click to expand setup instructions

Prerequisites

Java 11 or higher: Ensure Java Development Kit (JDK) is installed.
Maven: For dependency management and building the project.
MySQL: Running locally or remotely (version 8.0 or higher recommended).
Postman (optional): For testing API endpoints.

Installation Steps

Clone the Repository:
git clone https://github.com/Rahul-raya/Inventory-Management.git
cd Inventory-Management


Configure MySQL:

Ensure MySQL is running.
Create a database (e.g., inventory_db).
Update the application.properties file in src/main/resources with your MySQL credentials:spring.datasource.url=jdbc:mysql://localhost:3306/inventory_db
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update




Build the Project:
mvn clean install


Run the Application:
mvn spring-boot:run

The application will start on http://localhost:8080.




Usage 🚀

Click to expand usage instructions


Access the API: The application runs on http://localhost:8080/api/inventory.
Test with Postman:
Import the Postman collection (if provided) or manually test the endpoints.
Example endpoints:
GET /api/inventory: Retrieve all inventory items.
POST /api/inventory: Create a new inventory item.
PUT /api/inventory/{id}: Update an existing item.
DELETE /api/inventory/{id}: Delete an item.




Sample JSON Payload (for creating an item):{
  "name": "Sample Item",
  "quantity": 100,
  "price": 29.99
}





Contributing 🤝

Fork the repository.
Create a new branch (git checkout -b feature-branch).
Make your changes and commit (git commit -m "Add feature").
Push to the branch (git push origin feature-branch).
Create a Pull Request.

License 📄
This project is licensed under the MIT License - see the LICENSE file for details.
Contact 📬
For questions or feedback, reach out to Rahul Raya.
