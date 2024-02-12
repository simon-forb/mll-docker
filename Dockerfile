FROM jupyter/minimal-notebook:notebook-6.4.12
# Inspect the Dockerfile at:
# https://github.com/jupyter/docker-stacks/tree/HEAD/minimal-notebook/Dockerfile

USER root

RUN chown -R jovyan /home/jovyan

RUN apt update && apt -y install pkg-config libcairo2-dev gcc libgirepository1.0-dev git clang \
    llvm graphviz openjdk-21-jdk vim

RUN pip install --no-cache-dir pkgconfig black cairocffi graphviz importmagic matplotlib numpy \
    pandas PyGObject pyparsing python-crfsuite python-mnist pystan scikit-learn scikit-optimize \
    scipy sortedcontainers tabulate torch nbgitpuller
RUN pip install --no-cache-dir torch torchvision torchaudio \
    --extra-index-url https://download.pytorch.org/whl/cpu
RUN pip install ipympl nodejs
RUN jupyter labextension install @jupyter-widgets/jupyterlab-manager
RUN jupyter labextension install jupyter-matplotlib

# Install iJava kernel for juypter
RUN mkdir /tmp/ijava
RUN wget https://github.com/SpencerPark/IJava/releases/download/v1.3.0/ijava-1.3.0.zip -P /tmp/ijava
RUN unzip /tmp/ijava/ijava-1.3.0.zip -d /tmp/ijava
RUN python /tmp/ijava/install.py --classpath "/home/jovyan/mll/lib/*.jar:/home/jovyan/mll/bin/"
RUN rm -rf /tmp/ijava

USER jovyan
EXPOSE 8888
