# Smartlinx APP - RESTful API

<a href="https://www.java.com" target="_blank" rel="noreferrer"> 
    <img src="https://raw.githubusercontent.com/devicons/devicon/master/icons/java/java-original.svg" alt="java" width="40" height="40"/> 
  </a> 
 <a href="https://www.mysql.com/" target="_blank" rel="noreferrer"> 
    <img src="https://raw.githubusercontent.com/devicons/devicon/master/icons/mysql/mysql-original-wordmark.svg" alt="mysql" width="40" height="40"/> 
  </a>

This repository contains the implementation of the **Smartlinx** RESTful API. The API facilitates the management of smart devices through MQTT protocols, enabling remote control, configuration, and supervision of the devices.

## 📋 Table of Contents

1. [🚀 Key Features](#-key-features)
2. [🔧 Installation](#-installation)
3. [⚙️ Configuration](#-configuration)
4. [📄 Documentation](#-documentation)
5. [🌐 Main Endpoints](#-main-endpoints)
   - [🔑 Authentication](#-authentication)
   - [👤 User Management](#-user-management)
   - [🏠 Home Management](#-home-management)
   - [📱 Device Management](#-device-management)
6. [❗ Error Codes](#-error-codes)
7. [🤝 Contributing](#-contributing)

## 🚀 Key Features

- **User Management**: Register, update, and delete users.
- **Home Management**: Add and manage connected homes.
- **Device Management**: Add, update, and remove devices associated with homes.
- **MQTT Integration**: Support for device communication with hubs via MQTT.
- **Room and Family Management**: Create and manage rooms and family members.

## 🔧 Installation

1. Clone the repository:

   ```bash
   git clone https://github.com/your-username/smartlinx-app-api.git
   ```

2. Import the project into an IDE such as **IntelliJ** or **Eclipse**.

3. Ensure you have **Java 11** or later and **Maven** installed.

4. Build and run the project using Maven:

   ```bash
   mvn clean install
   mvn exec:java
   ```

5. The API will be available at `http://localhost:8080`.

## ⚙️ Configuration

You can configure the API settings by modifying the `application.properties` file:

```properties
server.port=8080
database.url=jdbc:mysql://localhost:3306/smartlinx
database.username=root
database.password=your_password
```

## 📄 Documentation

Complete API documentation is available in the following formats:

- **Javadoc**: [javadoc.smartlinx.it](http://javadoc.smartlinx.it)
- **PDF**: [SmartLinx_API Reference.pdf](https://smartlinx.it/Documentation/SmartLinx_API%20Reference.pdf)

## 🌐 Main Endpoints

### 🔑 Authentication

- **POST** `/auth/login`: Logs in and returns a JWT token.
- **POST** `/auth/register`: Registers a new user.

### 👤 User Management

- **POST** `/smartlinx/user`: Adds a new user.
- **GET** `/smartlinx/user`: Retrieves user information by email.
- **PUT** `/smartlinx/user`: Updates user information.
- **DELETE** `/smartlinx/user`: Deletes a user.

### 🏠 Home Management

- **POST** `/smartlinx/home`: Adds a new home.
- **GET** `/smartlinx/home`: Retrieves homes associated with a user.
- **PUT** `/smartlinx/home`: Updates home information.
- **DELETE** `/smartlinx/home`: Deletes a home.

### 📱 Device Management

- **POST** `/smartlinx/device`: Adds a new device to a room.
- **GET** `/smartlinx/device`: Retrieves devices in a room.
- **PUT** `/smartlinx/device`: Updates device information.
- **DELETE** `/smartlinx/device`: Deletes a device.

## ❗ Error Codes

- **400** Bad Request: The request was incorrect.
- **401** Unauthorized: Authentication token is missing or invalid.
- **404** Not Found: The resource was not found.
- **500** Internal Server Error: Server encountered an error.

## 🤝 Contributing

Contributions are welcome! Follow these steps to contribute:

1. Fork the repository.
2. Create a new branch: `git checkout -b feature/your-feature-name`.
3. Commit your changes: `git commit -m 'Add new feature'`.
4. Push the branch: `git push origin feature/your-feature-name`.
5. Open a pull request.
