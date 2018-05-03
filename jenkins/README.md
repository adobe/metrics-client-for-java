# Build

## OSX Setup

Install latest boot2docker

```
gem install boot2docker
boot2docker up
boot2docker shellinit
```

Check that it works by running `docker ps`

Install [buildrunner](https://git.corp.adobe.com/dms-release-engineering/buildrunner)

```syntax:bash
virtualenv buildrunner
source buildrunner/bin/activate
pip install -i https://pypi.dev.ut1.omniture.com/releng/pypi/ vcsinfo
pip install -i https://pypi.dev.ut1.omniture.com/releng/pypi/ buildrunner
```

## Run the build

`buildrunner -f jenkins/buildrunner.yaml`

## Debugging

To attach to an intermediate build step from the docker image with the platform repo mounted on the `/source` dir on the container, run the following:

```
docker run --rm -v /Users/<your_osx_user>/path/to/platform:/source -it 1a8423154d01 bash -il
```

The intermediate container id (`1a8423154d01`) can be found by looking at the buildrunner output

