# One-Two-Three Quarkus

This is a fun version of the ‘freeze or die’ game, where each phone represented a player on screen. By shaking your phone, you can advance your player towards the finish line… provided you don’t ‘die’ along the way!

It is dedicated to explaining how Quarkus simplifies the process of creating such an interactive and scalable game. The extensions involved are:

- [Web Bundler](https://docs.quarkiverse.io/quarkus-web-bundler/dev/index.html)
- [Qute](https://quarkus.io/guides/qute-reference)
- [Renarde](https://quarkiverse.github.io/quarkiverse-docs/quarkus-renarde/dev/index.html)
- [Playwright](https://docs.quarkiverse.io/quarkus-playwright/dev/) for testing.

## Dev Mode

One tab:
```shell
cd dashboard;
quarkus dev
```

Another tab:
```shell
cd runnner;
quarkus dev
```

Open dashboard on http://localhost:8079/
Open multiple runners on http://localhost:8080/

## Deploy to OpenShift

Create a Kafka Cluster, then (after logging with `oc` and selecting the project):


Deploy Dashboard (only one pod max):
```shell
cd dashboard
quarkus build -Dquarkus.kubernetes.deploy=true --clean
```

Get dashboard route:
```shell
oc get route one-two-three-quarkus-dashboard -o jsonpath='https://{.spec.host}/api/game'
```

Deploy Runners (scale to N):
```shell
cd ../runners
quarkus build -Dquarkus.kubernetes.deploy=true --clean -Dfrontend.api.game=DASHBOARD_API_GAME_URL
```

