## Table of contents

1. [Installing Docker](#1-install-docker)
2. [How to Setup](#2-how-to-setup)
3. [How to Use](#3-how-to-use)

## 1. Install Docker
Before starting the local development environment, you need to install Docker.

### Docker Installation - Windows
To use Docker on Windows install the Docker Desktop.
We encourage you to use the WSL2 (Windows Subsystem for Linux) as backend.
You can find the download link and corresponding installation instructions [here](https://docs.docker.com/desktop/install/windows-install/).

[https://docs.docker.com/desktop/install/windows-install/](https://docs.docker.com/desktop/install/windows-install/)

#### Troubleshooting WSL
Docker in the WSL can use up too many resources. We therefore limit the RAM 
usage with the following commands.

Create the file

```
C:\Users\<username>\.wslconfig
```

with the following content

```
[wsl2]
memory=3GB
```

You can adapt the memory usage to your system. 
Furthermore, you can limit the amount of processors used by `processors=1`.


#### Starting the Docker Engine
On Windows you always need to start Docker first manually.
Open Docker Desktop and click the little Docker icon in the bottom left corner 
to start the engine.

### Docker Installation - Mac

To use Docker on Mac install the Docker Desktop.
You can find the download link and corresponding installation instructions [here](https://docs.docker.com/desktop/install/mac-install/).

[https://docs.docker.com/desktop/install/mac-install/](https://docs.docker.com/desktop/install/mac-install/)


### Docker Installation - Linux
#### Installation using Snap
You can install docker using a single command on Ubuntu using Snap:

```
sudo snap install docker
```

#### Installation using apt-get
You can also install docker using apt-get. Please follow the official 
instuctions given [here](https://docs.docker.com/engine/install/ubuntu/).

[https://docs.docker.com/engine/install/ubuntu/](https://docs.docker.com/engine/install/ubuntu/)


## 2. How to Setup
### Clone this Repository
Clone this repository and go into the root directory of the repository by typing 
the following commands in a terminal:

```
git clone https://github.com/uma-pi1/ml-docker
cd ml-docker
```

Alternatively, you can click on the "Code" button on the top right of this page 
and click "Download ZIP". Then you need
to decompress the ZIP file into a new folder.

### Pull and Start the Docker Container
With an installed Docker environment and a started engine you can now run the 
Docker container by typing the following command in a terminal on the folder 
where you have this downloaded repository:

```
docker compose up
```

**Note: The first time you are running this command it will take some time depending on your notebook and internet connection.**
**It will only take that long the first time you run this command. All following start-ups should be quick.**

You will need to run this command in this folder everytime you want to start up 
the jupyter notebook for these courses. The data you created/modified in the
notebook the last time you used the notebook will be there the next time you
start it.

## 3. How to Use
Run the following command in the root folder of the repository to get the 
notebook started:

```
docker compose up
```

You can access JupyterLab on

[http://localhost:8888](http://localhost:8888)

**If you see a prompt asking you for a token or password type `ml`.**

### 7.1 Transfer Between Host and Notebook

All files placed in the folder `./shared` located in the root directory of this repository on your host machine will directly appear in your jupyter lab environment.
Vice versa, notebooks created in jupyter lab will directly be stored in the folder `./shared` on your host machine.

### 7.2 Misc

The default user name in JupyterLab is `jovyan`.

*""Jovyan is often a special term used to describe members of the Jupyter community. It is also used as the user ID in the Jupyter Docker stacks or referenced in conversations."*
For more information see [here](https://jupyter-docker-stacks.readthedocs.io/en/latest/using/faq.html#who-is-jovyan).
