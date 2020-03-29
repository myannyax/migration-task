plugins {
    java
}

version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testCompile("junit", "junit", "4.12")
    implementation("org.apache.directory.studio:org.apache.commons.io:2.4")

    implementation("com.squareup.retrofit2:retrofit:2.7.2")
    implementation("com.squareup.retrofit2:converter-jaxb:2.7.0")
    implementation("com.squareup.retrofit2:converter-jackson:2.7.2")
    implementation("com.squareup.okhttp3:okhttp:4.4.1")

    implementation("com.sun.activation:javax.activation:1.2.0")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_9
}

tasks.test {
    useJUnit()
}