FROM gitpod/workspace-full

RUN sudo rm -rf /usr/bin/hd && \
    brew install linuxsuren/linuxsuren/hd && \
    hd install cli/cli
