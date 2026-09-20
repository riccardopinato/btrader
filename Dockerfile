FROM ubuntu:24.04 AS build

ENV DEBIAN_FRONTEND=noninteractive
ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV PATH=/opt/gradle/bin:/opt/android-sdk/cmdline-tools/latest/bin:/opt/android-sdk/platform-tools:$PATH

RUN apt-get update && apt-get install -y --no-install-recommends \
    openjdk-17-jdk-headless curl unzip ca-certificates git \
    && rm -rf /var/lib/apt/lists/*

RUN mkdir -p /opt/android-sdk/cmdline-tools /opt/gradle /work
RUN curl -fsSL https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -o /tmp/cmdline.zip \
    && unzip -q /tmp/cmdline.zip -d /tmp/cmdline \
    && mv /tmp/cmdline/cmdline-tools /opt/android-sdk/cmdline-tools/latest \
    && rm -rf /tmp/cmdline /tmp/cmdline.zip

RUN yes | sdkmanager --sdk_root=/opt/android-sdk --licenses >/dev/null || true
RUN sdkmanager --sdk_root=/opt/android-sdk \
    "platform-tools" \
    "platforms;android-36" \
    "build-tools;36.0.0"

RUN curl -fsSL https://services.gradle.org/distributions/gradle-9.3.1-bin.zip -o /tmp/gradle.zip \
    && unzip -q /tmp/gradle.zip -d /opt \
    && mv /opt/gradle-9.3.1/* /opt/gradle/ \
    && rmdir /opt/gradle-9.3.1 \
    && rm /tmp/gradle.zip

COPY easydebloat-ci/source.part00 /tmp/source.part00
COPY easydebloat-ci/source.part01 /tmp/source.part01
COPY easydebloat-ci/source.part02 /tmp/source.part02
COPY easydebloat-ci/source.part03 /tmp/source.part03
COPY easydebloat-ci/source.part04 /tmp/source.part04

RUN cat /tmp/source.part00 /tmp/source.part01 /tmp/source.part02 /tmp/source.part03 /tmp/source.part04 \
    | base64 -d > /tmp/easydebloat.tar.gz \
    && tar -xzf /tmp/easydebloat.tar.gz -C /work

WORKDIR /work/EasyDebloat_v0.1.0

RUN sed -i 's/compileSdk = 37/compileSdk = 36/' app/build.gradle.kts
RUN gradle --version
RUN gradle testDebugUnitTest assembleDebug --stacktrace

FROM python:3.13-alpine
WORKDIR /srv
COPY --from=build /work/EasyDebloat_v0.1.0/app/build/outputs/apk/debug/app-debug.apk /srv/EasyDebloat-v0.1.0-debug.apk
EXPOSE 8080
CMD ["python", "-m", "http.server", "8080", "--bind", "0.0.0.0"]
