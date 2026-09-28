tasks.register<Exec>("assembleDebug") {
    workingDir(projectDir)
    commandLine("npm", "run", "build")
}

tasks.register<Exec>("build") {
    workingDir(projectDir)
    commandLine("npm", "run", "build")
}
