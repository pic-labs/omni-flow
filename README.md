# Omni-Flow

<div align="center">

English / [简体中文](./README_cn.md)

</div>

## Table of Contents
- [Introduction](#introduction)
- [Environment Requirements](#environment-requirements)
- [Installation](#installation)
- [Usage](#usage)
- [Contribution](#contribution)
- [License](#license)

## Introduction
Omni-Flow is a controller for orchestrating and scheduling AI capabilities, aiming to simplify the management and task scheduling of AI models. Through JSON configuration files and API calls, Omni-Flow can flexibly schedule various AI models to generate required media content. This project draws inspiration from Kubernetes and is built based on the Spring Boot framework, with Redis used as the underlying storage.

## Environment Requirements
To run this project, you need the following environment:
- **Java**: Version 21 or higher
- **Maven**: Version 3.x or higher
- **Redis**: Version 5.0 or higher (Stream support required)
- **Docker** (optional): For containerized deployment

## Installation
### Deploy Using Docker
1. Build Docker image:

```shell
docker build -t omni-flow .
```

2. Run Docker container:

```shell
docker run -d --name omni-flow -p 8080:8080 omni-flow
```

### Local Build and Run
1. Clone repository:
```shell
git clone https://github.com/your-repo/omni-flow.git
cd omni-flow
```

2. Build project:
```shell
mvn clean install
```

3. Start application:
```shell
java -jar target/omni-flow-0.0.1-SNAPSHOT.jar
```

## Usage
### API Documentation
The project integrates OpenAPI documentation, which can be accessed and tested via `http://localhost:8080/swagger-ui.html`.

#### Example Request
You can send HTTP requests to the service using the following command:
```shell
curl -X GET http://localhost:8080/api/xxxx
```

### Portal User Guide
Omni-Flow provides a Web Portal for managing and monitoring the creation and execution of AI tasks. Below are the steps to access and use the Portal:

#### Accessing Portal
1. **Start Application**: Ensure that the Omni-Flow application has started successfully.
2. **Open Browser**: Enter the following URL in your browser to access the Portal:
```text
http://localhost:8080/page/controlplane.html
```

#### Main Function Modules
The Portal includes several main function modules:

1. **Kind Create**
   - **Define Kind**: Create new Kind tasks by filling out definitions in JSON format.
   - **Sample Button**: Click the "Coze Sample" button to load a sample definition.
   - **Create Button**: Click the "Create" button to submit the definition and create a new Kind task.

2. **Kind List**
   - **Search Functionality**: Input UserId and click the "Search" button to query the Kind task list for specific users.
   - **Task List**: Display all Kind tasks in table form, including ID, Phase, Kind, and creation time information.
   - **Pagination Navigation**: Use pagination buttons to browse through different pages of the task list.

3. **Detail Pop-up**
   - **View Details**: Click on a row in the task list to pop up a detail window showing detailed information about the task.
   - **JSON View**: Click the "Json" button to view the task's JSON definition and status.
   - **Regenerate**: Select different generation steps and click the "Regenerate" button to regenerate the task.

## Contribution
Contributions to Omni-Flow are welcome! Please follow these steps:
1. Fork this repository.
2. Create a new branch (`git checkout -b feature/new-feature`).
3. Commit your changes (`git commit -am 'Add some feature'`).
4. Push to the branch (`git push origin feature/new-feature`).
5. Initiate a Pull Request.

## License
The Omni-Flow project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

---

Thank you for your attention and support for Omni-Flow! If you have any questions or suggestions, please feel free to contact our development team.