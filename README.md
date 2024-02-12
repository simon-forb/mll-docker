# MLL-Docker - Machine Learning Language Docker Image & Container

This repository contains all files which are required to run the MLL Java package.

## Requirements

The following software products are required:

- A Java Development Kit (JDK) such as [OpenJDK](https://jdk.java.net/21/). Preferably install through a package manager like `apt` on UNIX-like systems.
- A Java IDE such as [Eclipse](https://eclipseide.org/).
- A [Docker](https://www.docker.com/products/docker-desktop/) installation.

## Instructions

Start by setting up the project locally:

- Open the project `./shared/mll` in Eclipse.
- Configure build path:
    - Add every `.jar` file in the directory `lib` to classpath.
    - Add `graphviz-java` as module.
    - Add `xchart` as module.
- At this point, Eclipse should be able to build and run the project.

Now add the Docker features:

- In any shell, change the working directory to the root of this repository.
- Execute `docker-compose up -d`. The first run might require some time (~ 15 min). Subsequent runs will be much faster.

Run `docker-compose down` to stop the docker container when finished with working on this project.


## Notes

- Code from the package `mll` will be available in a Jupyter Notebook **once it is compiled to .class files and stored in the** `shared/mll/bin` **directory.** The package needs to be imported using `import mll.*` in any Jupyter Notebook.
- Any change to these `.class` files requires a kernel restart.
