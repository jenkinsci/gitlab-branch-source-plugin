#!groovy

def configurations = [
    [ platform: "linux", jdk: "17", jenkins: null ],
    [ platform: "windows", jdk: "17", jenkins: null ],
    [ platform: "linux", jdk: "21", jenkins: null ],
]
buildPlugin(useContainerAgent: true, configurations: configurations)
