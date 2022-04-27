## Git Account Registration Spinnaker Plugin
This plugin clones a remote Git Repository and reads a Yml/JSON file for loading GCE account dynamically. Accounts are added and/or removed without restarting clouddriver service.


### Introduction
1. Clouddriver poller syncs  with a configured Git repository to update Spinnaker GCE accounts. Supports account addition, removal, and update.
2. Dynamic accounts loading - The plugin will perform a sync operation with configured Git respository to fetch accounts details from a file at runtime.


### Requirements
1. Must be used with Spinnaker version 1.24 (TODO) or higher.
2. Clouddriver must have support for Dynamic Accounts Loading [GCE support](https://github.com/kirangodishala/clouddriver/tree/1.26.x-external-accounts-support)
3. Must have a Git repository with credentials, filename to access the Yml/JSON file to load GCE accounts.


### Fetches and Loads GCE Account from a file in a Git repository
This plugin fetches GCE accounts from a Yml/JSON file stored in a Git repository.


```yaml
google:
   enabled: true
   accounts:
      - name: spinnaker-gce-account-v1.2
        requiredGroupMembership: []
        permissions:
           WRITE:
              - compute.projects.write
        project: opsmx-ggproject-2022
        jsonPath: encryptedFile:gcs!b:gce-accounts-v1!f:gce-account.json
        alphaListed: false
        imageProjects: []
        consul:
           enabled: false
           agentEndpoint: localhost
           agentPort: 8500
           datacenters: []
   primaryAccount: spinnaker-gce-account-v1.2
   bakeryDefaults:
      templateFile: gce.json
      baseImages: []
      zone: us-central1-f
      network: default
      useInternalIp: false
```


### Note
1. Plugin clones a Git repository using the Git credentials, provided in plugin configuration and fetches a filename (Yml/JSON). 
   Expectation is that the remote host will return accounts that were updated after the specified time by the field.
   This is done to avoid returning and processing all accounts every time sync occurs.
   Attribute jsonPath of Google Account is stored as an encrtyped secret file. [profile](https://spinnaker.io/docs/reference/halyard/secrets/gcs-secrets/)
   E.g. ```jsonPath: encryptedFile:gcs!b:gce-accounts-v1!f:gce-account.json```
2. Google Account are added/removed dynamically without restarting any services.


### Build and Load Plugin
1. To build the plugin run the following command `./gradlew clean build`.
2. To build the plugin zip run the following command `./gradlew releaseBundle`.
   The above command will produce a zip file, `build/distributions/git-account-registration-plugin*.zip`.
3. Publish the release at GitHub site and update `plugins.json` with the URL of plugin zip. or
   Copy the zip file to Clouddriver plugin directory. Defaults to `/opt/clouddriver/plugins`. This directory can be specified by the `plugins-root-path` configuration property.
4. Enable the plugin by placing the following in [Clouddriver profile](https://spinnaker.io/reference/halyard/custom/#custom-profiles)


### Setup
1. Add the following to `clouddriver.yml` in the necessary [profile](https://spinnaker.io/reference/halyard/custom/#custom-profiles) to load plugin.
```yaml
spinnaker:
   extensibility:
      plugins-root-path: /opt/clouddriver/plugins
      plugins:
         Git.AccountRegistration:
            id: Git.AccountRegistration
            enabled: true
            version: 0.0.1
            extensions: {}
            config: {}
      repositories:
         gitAccountRegistrationPluginRepo:
            id: gitAccountRegistrationPluginRepo
            url: https://raw.githubusercontent.com/ashish-ck/git-accounts-yml/master/repositories.json

config:
   repositoryName: < REPOSITORY NAME >
   filename: < FILENAME >
   gitHttpsUsername: < GIT USERNAME >
   gitHttpsPassword: < GIT PASSWORD >
   githubOAuthAccessToken: < GIT OAUTH TOKEN >
   sshPrivateKeyFilePath: < PRIVATE KEY FILE PATH >
   sshPrivateKeyPassphrase: < PARAPHRASE >
   sshKnownHostsFilePath: < KNOWN HOST FILE PATH >
   credentialType: < HTTPS_GITHUB_OAUTH_TOKEN /  HTTPS_USERNAME_PASSWORD /  SSH / NONE >
   sshTrustUnknownHosts: < true / false >

credentials:
   poller:
      enabled: true
      types:
         gce: # Enable GCE dynamic accounts registration poller
            reloadFrequencyMs: 20000 # Reload frequency in milliseconds
```


## License
This project is licensed under the Apache-2.0 License.

